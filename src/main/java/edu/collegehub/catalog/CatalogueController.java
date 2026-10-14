package edu.collegehub.catalog;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogueController {
    private final CatalogueService catalogue;
    public CatalogueController(CatalogueService catalogue) { this.catalogue = catalogue; }

    @GetMapping("/programmes/{programmeId}/semesters")
    public List<Map<String, Object>> semesters(@PathVariable Long programmeId) {
        return catalogue.semesterOptions(programmeId).stream().map(s -> Map.<String, Object>of("id", s.id(), "number", s.number(), "label", s.label())).toList();
    }
    @GetMapping("/programmes/{programmeId}/subjects")
    public List<Map<String, Object>> subjects(@PathVariable Long programmeId) {
        return catalogue.subjectOptions(programmeId, null).stream().map(s -> Map.<String, Object>of("id", s.id(), "name", s.name(), "code", s.code(), "semesterId", s.semesterId())).toList();
    }
    @GetMapping("/semesters/{semesterId}/subjects")
    public List<Map<String, Object>> subjectsInSemester(@PathVariable Long semesterId) {
        return catalogue.subjectOptionsForSemester(semesterId).stream()
                .map(subject -> Map.<String, Object>of("id", subject.id(), "name", subject.name(), "code", subject.code(), "semesterId", subject.semesterId())).toList();
    }
    @GetMapping("/subjects/suggestions")
    public List<CatalogueService.SubjectSuggestion> subjectSuggestions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Long programmeId,
            @RequestParam(required = false) Long semesterId) {
        return catalogue.subjectSuggestions(q, programmeId, semesterId);
    }
}
