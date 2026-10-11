package edu.collegehub.home;

import java.util.List;

import edu.collegehub.accounts.AppUser;
import edu.collegehub.accounts.AppUserRepository;
import edu.collegehub.catalog.CatalogueService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final CatalogueService catalogue;
    private final AppUserRepository users;

    public HomeController(CatalogueService catalogue, AppUserRepository users) {
        this.catalogue = catalogue; this.users = users;
    }

    @GetMapping("/")
    @Transactional(readOnly = true)
    public String home(@AuthenticationPrincipal AppUser principal, Model model) {
        model.addAttribute("programmes", catalogue.programmes());
        model.addAttribute("subjectCards", catalogue.programmes().stream()
                .flatMap(programme -> catalogue.subjects(programme.getId(), null).stream()
                        .map(subject -> new SubjectCard(subject.getId(), programme.getId(), programme.getName(),
                                programme.getCode(), subject.getSemester().getId(), subject.getName(), subject.getCode())))
                .toList());
        if (principal != null) {
            AppUser user = users.findById(principal.getId()).orElseThrow();
            if (user.getPreferredProgramme() != null) {
                model.addAttribute("preferredProgrammeId", user.getPreferredProgramme().getId());
                model.addAttribute("preferredSemesterId", user.getCurrentSemester() == null ? null : user.getCurrentSemester().getId());
                model.addAttribute("preferredSemesters", catalogue.semesterOptions(user.getPreferredProgramme().getId()));
            }
        }
        model.addAttribute("demoCategories", true);
        return "home";
    }

    public record SubjectCard(Long subjectId, Long programmeId, String programmeName, String programmeCode,
                              Long semesterId, String subjectName, String subjectCode) { }
}
