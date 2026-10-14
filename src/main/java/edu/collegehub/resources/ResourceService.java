package edu.collegehub.resources;

import java.util.List;

import edu.collegehub.accounts.AccountRole;
import edu.collegehub.accounts.AppUser;
import edu.collegehub.accounts.AppUserRepository;
import edu.collegehub.catalog.CatalogueService;
import edu.collegehub.catalog.Programme;
import edu.collegehub.catalog.Semester;
import edu.collegehub.catalog.Subject;
import edu.collegehub.usage.UsageEvent;
import edu.collegehub.usage.UsageEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResourceService {
    private final ResourceRepository resources;
    private final UsageEventRepository events;
    private final AppUserRepository users;
    private final CatalogueService catalogue;
    private final UploadStorageService storage;

    public ResourceService(ResourceRepository resources, UsageEventRepository events, AppUserRepository users,
                           CatalogueService catalogue, UploadStorageService storage) {
        this.resources = resources; this.events = events; this.users = users; this.catalogue = catalogue; this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Page<Resource> search(Long programmeId, Long semesterId, Long subjectId, ResourceType type,
                                 Integer examYear, ExamType examType, String query, boolean papersOnly, Pageable page) {
        return resources.findAll(ResourceSearch.approved(programmeId, semesterId, subjectId, type,
                examYear, examType, query, papersOnly), page);
    }
    @Transactional(readOnly = true)
    public Resource publicResource(Long id) {
        return resources.findByIdAndStatusAndRemovedFalseAndDemoResourceFalse(id, ModerationStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException());
    }
    @Transactional(readOnly = true)
    public List<Integer> approvedQuestionPaperYears() { return resources.findApprovedQuestionPaperYears(); }
    @Transactional
    public Resource create(UploadForm form, AppUser principal) {
        if (!form.isPermissionConfirmed()) throw new IllegalArgumentException("Confirm that you have permission to share this material.");
        Selected selected = select(form);
        validateExam(form);
        UploadStorageService.ValidatedUpload validated = storage.validate(form.getFile());
        UploadStorageService.StoredUpload stored = storage.store(validated);
        AppUser uploader = users.findById(principal.getId()).orElseThrow();
        Resource resource = new Resource();
        resource.setDetails(form.getTitle(), form.getDescription(), selected.programme(), selected.semester(), selected.subject(),
                form.getResourceType(), stored.format(), stored.displayFilename(), stored.storedFilename(), stored.size(),
                paperYear(form), paperExamType(form), paperSession(form));
        resource.setUploader(uploader);
        resources.save(resource);
        events.save(new UsageEvent("UPLOAD", resource, uploader));
        return resource;
    }

    @Transactional
    public Resource update(Long id, UploadForm form, AppUser principal) {
        Resource resource = resources.findByIdAndUploadedBy_IdAndRemovedFalse(id, principal.getId())
                .orElseThrow(ResourceNotFoundException::new);
        if (!form.isPermissionConfirmed()) {
            throw new IllegalArgumentException("Confirm that you have permission to share this material.");
        }
        Selected selected = select(form);
        validateExam(form);
        boolean hasNewFile = form.getFile() != null && !form.getFile().isEmpty();
        UploadStorageService.ValidatedUpload validated = hasNewFile ? storage.validate(form.getFile()) : null;
        if (!hasNewFile && resource.getFileFormat() == null) throw new IllegalArgumentException("Choose a file to upload.");
        String oldFile = resource.getStoredFilename();
        UploadStorageService.StoredUpload stored = hasNewFile ? storage.store(validated) : null;
        resource.setDetails(form.getTitle(), form.getDescription(), selected.programme(), selected.semester(), selected.subject(),
                form.getResourceType(), hasNewFile ? stored.format() : resource.getFileFormat(),
                hasNewFile ? stored.displayFilename() : resource.getDisplayFilename(),
                hasNewFile ? stored.storedFilename() : null,
                hasNewFile ? stored.size() : -1,
                paperYear(form), paperExamType(form), paperSession(form));
        if (hasNewFile && oldFile != null) storage.deleteAfterCommit(storage.pathFor(oldFile));
        AppUser uploader = users.findById(principal.getId()).orElseThrow();
        events.save(new UsageEvent("UPLOAD", resource, uploader));
        return resource;
    }

    @Transactional(readOnly = true)
    public List<Resource> myResources(Long userId) {
        AppUser user = users.findById(userId).orElseThrow();
        return resources.findByUploadedByOrderByUploadedAtDesc(user);
    }
    @Transactional(readOnly = true)
    public Resource ownedResource(Long resourceId, Long userId) {
        return resources.findByIdAndUploadedBy_IdAndRemovedFalse(resourceId, userId)
                .orElseThrow(ResourceNotFoundException::new);
    }
    @Transactional(readOnly = true)
    public Resource moderationResource(Long id) {
        return resources.findById(id).filter(resource -> !resource.isRemoved())
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public void remove(Long id, AppUser principal) {
        Resource resource = resources.findByIdAndUploadedBy_IdAndRemovedFalse(id, principal.getId())
                .orElseThrow(ResourceNotFoundException::new);
        if (resource.getStatus() == ModerationStatus.APPROVED) {
            throw new IllegalArgumentException("Approved resources cannot be removed from My uploads. Ask an administrator for help.");
        }
        resource.markRemoved();
        storage.deleteAfterCommit(storage.pathFor(resource.getStoredFilename()));
    }

    @Transactional
    public void removeByAdmin(Long id, String reason, AppUser principal) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.length() < 3 || normalizedReason.length() > 500) {
            throw new IllegalArgumentException("Enter a removal reason between 3 and 500 characters.");
        }
        if (principal == null || principal.getRole() != AccountRole.ADMIN) {
            throw new AccessDeniedException("Only an administrator can remove a resource.");
        }
        Resource resource = resources.findById(id).filter(r -> !r.isRemoved())
                .orElseThrow(ResourceNotFoundException::new);
        AppUser admin = users.findById(principal.getId()).orElseThrow();
        resource.markRemovedByAdmin(admin, normalizedReason);
        storage.deleteAfterCommit(storage.pathFor(resource.getStoredFilename()));
    }

    @Transactional(readOnly = true)
    public List<Resource> pendingResources() {
        return resources.findByStatusAndRemovedFalseOrderByUploadedAtAsc(ModerationStatus.PENDING);
    }

    @Transactional
    public void approve(Long id, AppUser principal) {
        Resource resource = resources.findById(id).filter(r -> !r.isRemoved() && r.getStatus() == ModerationStatus.PENDING)
                .orElseThrow(ResourceNotFoundException::new);
        AppUser admin = users.findById(principal.getId()).orElseThrow();
        resource.approve(admin);
    }

    @Transactional
    public void reject(Long id, String reason, AppUser principal) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.length() < 3 || normalizedReason.length() > 500) {
            throw new IllegalArgumentException("Enter a rejection reason between 3 and 500 characters.");
        }
        Resource resource = resources.findById(id).filter(r -> !r.isRemoved() && r.getStatus() == ModerationStatus.PENDING)
                .orElseThrow(ResourceNotFoundException::new);
        AppUser admin = users.findById(principal.getId()).orElseThrow();
        resource.reject(admin, normalizedReason);
    }

    @Transactional
    public DownloadedResource recordDownload(Long id, AppUser actor) {
        Resource resource = publicResource(id);
        resource.recordDownload();
        AppUser storedActor = actor == null ? null : users.findById(actor.getId()).orElse(null);
        events.save(new UsageEvent("DOWNLOAD", resource, storedActor));
        return new DownloadedResource(resource.getStoredFilename(), resource.getDisplayFilename(), resource.getFileFormat());
    }

    private Selected select(UploadForm form) {
        Programme programme = catalogue.requireProgramme(form.getProgrammeId());
        Semester semester = catalogue.requireSemester(programme.getId(), form.getSemesterId());
        Subject subject = catalogue.resolveSubjectName(programme, semester, form.getSubjectName());
        return new Selected(programme, semester, subject);
    }
    private void validateExam(UploadForm form) {
        if (form.getResourceType() == ResourceType.QUESTION_PAPER) {
            if (form.getExamYear() == null) throw new IllegalArgumentException("Enter the exam year for a question paper.");
            if (form.getExamType() == null) throw new IllegalArgumentException("Choose Mid-term, End-term, or Other.");
            if (form.getExamType() == ExamType.OTHER && (form.getExamSession() == null || form.getExamSession().isBlank())) {
                throw new IllegalArgumentException("Describe the exam type when you choose Other.");
            }
        }
    }
    private Integer paperYear(UploadForm form) { return form.getResourceType() == ResourceType.QUESTION_PAPER ? form.getExamYear() : null; }
    private ExamType paperExamType(UploadForm form) { return form.getResourceType() == ResourceType.QUESTION_PAPER ? form.getExamType() : null; }
    private String paperSession(UploadForm form) { return form.getResourceType() == ResourceType.QUESTION_PAPER ? form.getExamSession() : null; }

    public record DownloadedResource(String storedFilename, String displayFilename, FileFormat format) { }
    private record Selected(Programme programme, Semester semester, Subject subject) { }
}
