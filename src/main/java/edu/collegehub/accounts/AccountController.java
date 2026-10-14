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
        model.addAttribute("programmes", catalogue.programmesWithSemesters());
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
            programme = catalogue.activeProgramme(registrationForm.getProgrammeId());
            if (programme == null) {
                bindingResult.rejectValue("programmeId", "invalid", "Choose a valid programme.");
            } else {
                semester = catalogue.semesterInProgramme(registrationForm.getSemesterId(), programme.getId());
                if (semester == null) {
                    bindingResult.rejectValue("semesterId", "invalid",
                            "Choose a semester belonging to the selected programme.");
                }
            }
        } else if (registrationForm.getProgrammeId() != null) {
            bindingResult.rejectValue("semesterId", "requiredForProgramme",
                    "Choose a current semester for this programme, or clear the programme.");
        } else if (registrationForm.getSemesterId() != null) {
            bindingResult.rejectValue("programmeId", "requiredForSemester",
                    "Choose the programme that this semester belongs to.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("programmes", catalogue.programmesWithSemesters());
            Long programmeId = registrationForm.getProgrammeId();
            java.util.List<CatalogueService.SemesterOption> semesterOptions = programmeId == null
                    ? java.util.List.of() : catalogue.semesterOptions(programmeId);
            if (programmeId == null && registrationForm.getSemesterId() != null) {
                Semester submittedSemester = catalogue.semester(registrationForm.getSemesterId());
                if (submittedSemester != null) {
                    semesterOptions = java.util.List.of(new CatalogueService.SemesterOption(submittedSemester.getId(),
                            submittedSemester.getNumber(), submittedSemester.getLabel()));
                }
            }
            model.addAttribute("semesters", semesterOptions);
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
