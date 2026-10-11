package edu.collegehub.resources;

import java.util.List;

import edu.collegehub.accounts.AppUser;
import edu.collegehub.accounts.AppUserRepository;
import edu.collegehub.catalog.CatalogueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UploadController {
    private final ResourceService resources;
    private final CatalogueService catalogue;
    private final AppUserRepository users;
    private final long maxUploadSize;

    public UploadController(ResourceService resources, CatalogueService catalogue, AppUserRepository users,
                            @Value("${collegehub.upload.max-size-bytes:20971520}") long maxUploadSize) {
        this.resources = resources; this.catalogue = catalogue; this.users = users; this.maxUploadSize = maxUploadSize;
    }

    @GetMapping("/uploads/new")
    public String newUpload(@AuthenticationPrincipal AppUser principal,
                            @RequestParam(required = false) String tooLarge,
                            @RequestParam(required = false) ResourceType type, Model model) {
        UploadForm form = new UploadForm();
        form.setResourceType(type);
        AppUser user = users.findById(principal.getId()).orElseThrow();
        if (user.getPreferredProgramme() != null) {
            form.setProgrammeId(user.getPreferredProgramme().getId());
            if (user.getCurrentSemester() != null) form.setSemesterId(user.getCurrentSemester().getId());
        }
        model.addAttribute("uploadForm", form);
        model.addAttribute("editing", false);
        model.addAttribute("tooLarge", tooLarge != null || Boolean.TRUE.equals(model.getAttribute("tooLarge")));
        populateFormOptions(form, model);
        return "upload-form";
    }

    @PostMapping("/uploads/new")
    public String create(@AuthenticationPrincipal AppUser principal,
                         @Valid @ModelAttribute("uploadForm") UploadForm form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        validateCatalogueAndPaper(form, binding);
        if (form.getFile() == null || form.getFile().isEmpty()) binding.rejectValue("file", "required", "Choose a file to upload.");
        if (!form.isPermissionConfirmed()) binding.rejectValue("permissionConfirmed", "required", "Confirm that you have permission to share this material.");
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            model.addAttribute("fileNeedsReselect", form.getFile() != null && !form.getFile().isEmpty());
            populateFormOptions(form, model);
            return "upload-form";
        }
        try {
            resources.create(form, principal);
            redirect.addFlashAttribute("uploadSuccess", true);
            return "redirect:/my-uploads";
        } catch (IllegalArgumentException ex) {
            binding.reject("upload", ex.getMessage());
            model.addAttribute("editing", false);
            model.addAttribute("fileNeedsReselect", form.getFile() != null && !form.getFile().isEmpty());
            populateFormOptions(form, model);
            return "upload-form";
        }
    }

    @GetMapping("/my-uploads")
    public String myUploads(@AuthenticationPrincipal AppUser user,
                            @RequestParam(required = false) String uploaded,
                            @RequestParam(required = false) String updated,
                            Model model) {
        model.addAttribute("myResources", resources.myResources(user.getId()));
        model.addAttribute("uploadSuccess", uploaded != null || Boolean.TRUE.equals(model.getAttribute("uploadSuccess")));
        model.addAttribute("updateSuccess", updated != null);
        return "my-uploads";
    }

    @GetMapping("/my-uploads/{id}/edit")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal AppUser user, Model model) {
        Resource resource = resources.ownedResource(id, user.getId());
        UploadForm form = toForm(resource);
        model.addAttribute("uploadForm", form);
        model.addAttribute("resourceId", id);
        model.addAttribute("existingFilename", resource.getDisplayFilename());
        model.addAttribute("editing", true);
        model.addAttribute("fileNeedsReselect", false);
        populateFormOptions(form, model);
        return "upload-form";
    }

    @PostMapping("/my-uploads/{id}/edit")
    public String update(@PathVariable Long id, @AuthenticationPrincipal AppUser user,
                         @Valid @ModelAttribute("uploadForm") UploadForm form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        validateCatalogueAndPaper(form, binding);
        if (!form.isPermissionConfirmed()) binding.rejectValue("permissionConfirmed", "required", "Confirm that you have permission to share this material.");
        if (binding.hasErrors()) {
            model.addAttribute("resourceId", id);
            model.addAttribute("editing", true);
            model.addAttribute("fileNeedsReselect", form.getFile() != null && !form.getFile().isEmpty());
            resources.ownedResource(id, user.getId());
            populateFormOptions(form, model);
            return "upload-form";
        }
        try {
            resources.update(id, form, user);
            redirect.addFlashAttribute("updateSuccess", true);
            return "redirect:/my-uploads?updated";
        } catch (IllegalArgumentException ex) {
            binding.reject("upload", ex.getMessage());
            model.addAttribute("resourceId", id);
            model.addAttribute("editing", true);
            model.addAttribute("fileNeedsReselect", form.getFile() != null && !form.getFile().isEmpty());
            model.addAttribute("existingFilename", resources.ownedResource(id, user.getId()).getDisplayFilename());
            populateFormOptions(form, model);
            return "upload-form";
        }
    }

    @PostMapping("/my-uploads/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal AppUser user, RedirectAttributes redirect) {
        try {
            resources.remove(id, user);
            redirect.addFlashAttribute("removed", true);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("manageError", ex.getMessage());
        }
        return "redirect:/my-uploads";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("tooLarge", true);
        return "redirect:/uploads/new?tooLarge";
    }

    private void validateCatalogueAndPaper(UploadForm form, BindingResult binding) {
        try {
            if (form.getProgrammeId() != null && form.getSemesterId() != null && form.getSubjectId() != null) {
                catalogue.requireSubject(form.getProgrammeId(), form.getSemesterId(), form.getSubjectId());
            }
        } catch (IllegalArgumentException ex) {
            binding.reject("catalogue", ex.getMessage());
        }
        if (form.getResourceType() == ResourceType.QUESTION_PAPER) {
            if (form.getExamYear() == null) binding.rejectValue("examYear", "required", "Enter the exam year.");
            if (form.getExamType() == null) binding.rejectValue("examType", "required", "Choose an exam type.");
            if (form.getExamType() == ExamType.OTHER && (form.getExamSession() == null || form.getExamSession().isBlank())) {
                binding.rejectValue("examSession", "required", "Describe what kind of exam this was.");
            }
        }
    }

    private UploadForm toForm(Resource resource) {
        UploadForm form = new UploadForm();
        form.setTitle(resource.getTitle()); form.setDescription(resource.getDescription());
        form.setProgrammeId(resource.getProgramme().getId()); form.setSemesterId(resource.getSemester().getId());
        form.setSubjectId(resource.getSubject().getId()); form.setResourceType(resource.getResourceType());
        form.setExamYear(resource.getExamYear()); form.setExamType(resource.getExamType()); form.setExamSession(resource.getExamSession());
        form.setPermissionConfirmed(false);
        return form;
    }
    private void populateFormOptions(UploadForm form, Model model) {
        model.addAttribute("programmes", catalogue.programmes());
        model.addAttribute("semesters", catalogue.semesterOptions(form.getProgrammeId()));
        model.addAttribute("subjects", catalogue.subjectOptions(form.getProgrammeId(), form.getSemesterId()));
        model.addAttribute("resourceTypes", List.of(ResourceType.NOTES, ResourceType.QUESTION_PAPER, ResourceType.PRESENTATION,
                ResourceType.STUDY_GUIDE, ResourceType.PRACTICE_MATERIAL, ResourceType.LAB_PRACTICAL_MATERIAL));
        model.addAttribute("examTypes", List.of(ExamType.MID_TERM, ExamType.END_TERM, ExamType.OTHER));
        model.addAttribute("maxUploadSize", maxUploadSize);
    }
}
