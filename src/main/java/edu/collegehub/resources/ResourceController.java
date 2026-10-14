package edu.collegehub.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;

import edu.collegehub.accounts.AppUser;
import edu.collegehub.accounts.UserPreferenceService;
import edu.collegehub.catalog.CatalogueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class ResourceController {
    private static final int PAGE_SIZE = 10;
    private final ResourceService resourceService;
    private final CatalogueService catalogue;
    private final UploadStorageService storage;
    private final UserPreferenceService preferences;
    private final long maxUploadSize;

    public ResourceController(ResourceService resourceService, CatalogueService catalogue, UploadStorageService storage,
                              UserPreferenceService preferences,
                              @Value("${collegehub.upload.max-size-bytes:20971520}") long maxUploadSize) {
        this.resourceService = resourceService; this.catalogue = catalogue; this.storage = storage; this.preferences = preferences; this.maxUploadSize = maxUploadSize;
    }

    @GetMapping("/resources")
    public String browse(@RequestParam(required = false) Long programmeId,
                         @RequestParam(required = false) Long semesterId,
                         @RequestParam(required = false) Long subjectId,
                         @RequestParam(required = false) ResourceType type,
                         @RequestParam(required = false) Integer examYear,
                         @RequestParam(required = false) ExamType examType,
                         @RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "newest") String sort,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "false") boolean allCourses,
                         @AuthenticationPrincipal AppUser user,
                         Model model) {
        if (programmeId == null && !allCourses && user != null) {
            var preference = preferences.get(user.getId()); programmeId = preference.programmeId(); semesterId = preference.semesterId();
        }
        return renderBrowse(programmeId, semesterId, subjectId, type, examYear, examType, q, sort, page, false, allCourses, model);
    }

    @GetMapping("/question-papers")
    public String questionPapers(@RequestParam(required = false) Long programmeId,
                                 @RequestParam(required = false) Long semesterId,
                                 @RequestParam(required = false) Long subjectId,
                                 @RequestParam(required = false) Integer examYear,
                                 @RequestParam(required = false) ExamType examType,
                                 @RequestParam(defaultValue = "") String q,
                                 @RequestParam(defaultValue = "newest") String sort,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "false") boolean allCourses,
                                 @AuthenticationPrincipal AppUser user,
                                 Model model) {
        if (programmeId == null && !allCourses && user != null) {
            var preference = preferences.get(user.getId()); programmeId = preference.programmeId(); semesterId = preference.semesterId();
        }
        return renderBrowse(programmeId, semesterId, subjectId, ResourceType.QUESTION_PAPER,
                examYear, examType, q, sort, page, true, allCourses, model);
    }

    private String renderBrowse(Long programmeId, Long semesterId, Long subjectId, ResourceType type,
                                Integer examYear, ExamType examType, String q, String sort, int page,
                                boolean papersOnly, boolean explicitlyAllCourses, Model model) {
        boolean valid = catalogue.validSelection(programmeId, semesterId, subjectId);
        String safeSort = "most-downloaded".equals(sort) ? "most-downloaded" : "newest";
        int safePage = Math.max(0, Math.min(page, 10_000));
        Sort order = "most-downloaded".equals(safeSort)
                ? Sort.by(Sort.Order.desc("downloadCount"), Sort.Order.desc("uploadedAt"))
                : Sort.by(Sort.Order.desc("uploadedAt"));
        Pageable pageable = PageRequest.of(safePage, PAGE_SIZE, order);
        Page<Resource> result = valid
                ? resourceService.search(programmeId, semesterId, subjectId, type, examYear, examType, q, papersOnly, pageable)
                : new PageImpl<>(List.of(), pageable, 0);

        model.addAttribute("resourcePage", result);
        model.addAttribute("resources", result.getContent());
        model.addAttribute("programmes", catalogue.programmes());
        model.addAttribute("semesters", catalogue.semesterOptions(programmeId));
        model.addAttribute("selectedProgrammeName", catalogue.programmes().stream().filter(p -> p.getId().equals(programmeId)).map(p -> p.getName()).findFirst().orElse(""));
        model.addAttribute("selectedSemesterLabel", catalogue.semesterOptions(programmeId).stream().filter(s -> s.id().equals(semesterId)).map(s -> s.label()).findFirst().orElse(""));
        var selectedSubject = catalogue.subject(subjectId);
        model.addAttribute("selectedSubjectName", selectedSubject == null ? "" : selectedSubject.getName());
        model.addAttribute("paperYears", resourceService.approvedQuestionPaperYears());
        model.addAttribute("q", q);
        model.addAttribute("programmeId", programmeId);
        model.addAttribute("semesterId", semesterId);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("type", papersOnly ? ResourceType.QUESTION_PAPER : type);
        model.addAttribute("examYear", examYear);
        model.addAttribute("examType", examType);
        model.addAttribute("sort", safeSort);
        model.addAttribute("papersOnly", papersOnly);
        model.addAttribute("allCourses", explicitlyAllCourses || programmeId == null);
        model.addAttribute("validSelection", valid);
        model.addAttribute("browsePath", papersOnly ? "/question-papers" : "/resources");
        var requestUri = ServletUriComponentsBuilder.fromCurrentRequest().build();
        model.addAttribute("returnTo", requestUri.getPath() + (requestUri.getQuery() == null ? "" : "?" + requestUri.getQuery()));
        model.addAttribute("resourceTypes", List.of(ResourceType.values()));
        model.addAttribute("examTypes", List.of(ExamType.values()));
        model.addAttribute("maxUploadSize", maxUploadSize);
        return "resources";
    }

    @GetMapping("/resources/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) String returnTo, Model model) {
        Resource resource = resourceService.publicResource(id);
        model.addAttribute("resource", resource);
        model.addAttribute("backUrl", safeBackUrl(returnTo, resource));
        model.addAttribute("maxUploadSize", maxUploadSize);
        return "resource-detail";
    }

    private String safeBackUrl(String returnTo, Resource resource) {
        if (returnTo != null && !returnTo.startsWith("//") && !returnTo.contains("\\")
                && !returnTo.contains("\r") && !returnTo.contains("\n")) {
            String path = returnTo.split("\\?", 2)[0];
            if (path.equals("/resources") || path.equals("/question-papers")) return returnTo;
        }
        return "/resources?programmeId=" + resource.getProgramme().getId()
                + "&semesterId=" + resource.getSemester().getId()
                + "&subjectId=" + resource.getSubject().getId();
    }

    @GetMapping("/resources/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id,
                                                       @AuthenticationPrincipal AppUser user) {
        Resource approved = resourceService.publicResource(id);
        try {
            byte[] file = Files.readAllBytes(storage.pathFor(approved.getStoredFilename()));
            resourceService.recordDownload(id, user);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(approved.getFileFormat().getMediaType()))
                    .contentLength(file.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(approved.getDisplayFilename(), StandardCharsets.UTF_8).build().toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new ByteArrayResource(file));
        } catch (IOException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The resource file is no longer available.");
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String notFound(RedirectAttributes redirect) {
        redirect.addFlashAttribute("notFound", true);
        return "redirect:/resources";
    }
}
