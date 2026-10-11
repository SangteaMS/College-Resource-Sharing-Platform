package edu.collegehub.accounts;

import edu.collegehub.catalog.CatalogueService;
import edu.collegehub.catalog.Programme;
import edu.collegehub.catalog.Semester;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class AccountController {
    private final AppUserRepository users;
    private final CatalogueService catalogue;
    private final PasswordEncoder passwordEncoder;

    public AccountController(AppUserRepository users, CatalogueService catalogue, PasswordEncoder passwordEncoder) {
        this.users = users; this.catalogue = catalogue; this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String next, Model model) {
        model.addAttribute("loginError", error != null);
        model.addAttribute("loggedOut", logout != null);
        model.addAttribute("next", safeNext(next));
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        model.addAttribute("programmes", catalogue.programmes());
        return "register";
    }

    @PostMapping("/register")
    @Transactional
    public String createAccount(@Valid @ModelAttribute RegistrationForm registrationForm,
                                BindingResult bindingResult, Model model, RedirectAttributes redirect) {
        if (registrationForm.getEmail() != null && users.existsByEmailIgnoreCase(registrationForm.getEmail().trim())) {
            bindingResult.rejectValue("email", "duplicate", "An account already uses this email.");
        }
        Programme programme = null;
        Semester semester = null;
        if (registrationForm.getProgrammeId() != null && registrationForm.getSemesterId() != null) {
            try {
                programme = catalogue.requireProgramme(registrationForm.getProgrammeId());
                semester = catalogue.requireSemester(programme.getId(), registrationForm.getSemesterId());
            } catch (IllegalArgumentException ex) {
                bindingResult.reject("catalogue", ex.getMessage());
            }
        } else if (registrationForm.getProgrammeId() != null || registrationForm.getSemesterId() != null) {
            bindingResult.reject("catalogue", "Choose both a programme and a semester, or leave both blank.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("programmes", catalogue.programmes());
            return "register";
        }
        AppUser user = new AppUser(registrationForm.getEmail(), passwordEncoder.encode(registrationForm.getPassword()),
                registrationForm.getFullName(), AccountRole.STUDENT);
        if (programme != null) user.setPreferences(programme, semester);
        users.save(user);
        redirect.addFlashAttribute("accountCreated", true);
        return "redirect:/login?next=/uploads/new";
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public String profile(@AuthenticationPrincipal AppUser user, Model model) {
        AppUser account = users.findById(user.getId()).orElseThrow();
        model.addAttribute("programmes", catalogue.programmes());
        model.addAttribute("semesters", account.getPreferredProgramme() == null ? java.util.List.of() : catalogue.semesterOptions(account.getPreferredProgramme().getId()));
        model.addAttribute("preferredProgrammeId", account.getPreferredProgramme() == null ? null : account.getPreferredProgramme().getId());
        model.addAttribute("preferredSemesterId", account.getCurrentSemester() == null ? null : account.getCurrentSemester().getId());
        model.addAttribute("saved", false);
        return "profile";
    }

    @PostMapping("/profile")
    @Transactional
    public String saveProfile(@AuthenticationPrincipal AppUser principal,
                              @RequestParam(required = false) Long programmeId,
                              @RequestParam(required = false) Long semesterId,
                              Model model) {
        AppUser user = users.findById(principal.getId()).orElseThrow();
        if (programmeId == null && semesterId == null) {
            user.setPreferences(null, null);
        } else {
            try {
                Programme programme = catalogue.requireProgramme(programmeId);
                Semester semester = catalogue.requireSemester(programme.getId(), semesterId);
                user.setPreferences(programme, semester);
            } catch (IllegalArgumentException ex) {
                model.addAttribute("error", ex.getMessage());
                model.addAttribute("programmes", catalogue.programmes());
                model.addAttribute("semesters", catalogue.semesterOptions(programmeId));
                model.addAttribute("preferredProgrammeId", programmeId);
                model.addAttribute("preferredSemesterId", semesterId);
                return "profile";
            }
        }
        users.save(user);
        model.addAttribute("programmes", catalogue.programmes());
        model.addAttribute("semesters", user.getPreferredProgramme() == null ? java.util.List.of() : catalogue.semesterOptions(user.getPreferredProgramme().getId()));
        model.addAttribute("preferredProgrammeId", user.getPreferredProgramme() == null ? null : user.getPreferredProgramme().getId());
        model.addAttribute("preferredSemesterId", user.getCurrentSemester() == null ? null : user.getCurrentSemester().getId());
        model.addAttribute("saved", true);
        return "profile";
    }

    private String safeNext(String next) {
        return next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\") ? next : "";
    }
}
