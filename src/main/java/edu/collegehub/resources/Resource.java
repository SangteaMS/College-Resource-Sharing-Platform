package edu.collegehub.resources;

import java.time.LocalDateTime;

import edu.collegehub.accounts.AppUser;
import edu.collegehub.catalog.Programme;
import edu.collegehub.catalog.Semester;
import edu.collegehub.catalog.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "resources")
public class Resource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(length = 500)
    private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    @Enumerated(EnumType.STRING) @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;
    @Enumerated(EnumType.STRING) @Column(name = "file_format", nullable = false, length = 12)
    private FileFormat fileFormat;
    @Column(name = "display_filename", nullable = false, length = 255)
    private String displayFilename;
    @Column(name = "stored_filename", nullable = false, unique = true, length = 80)
    private String storedFilename;
    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;
    @Column(name = "exam_year")
    private Integer examYear;
    @Enumerated(EnumType.STRING) @Column(name = "exam_type", length = 20)
    private ExamType examType;
    @Column(name = "exam_session", length = 100)
    private String examSession;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ModerationStatus status = ModerationStatus.PENDING;
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "uploaded_by")
    private AppUser uploadedBy;
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "removed_by")
    private AppUser removedBy;
    @Column(name = "removed_at")
    private LocalDateTime removedAt;
    @Column(name = "removal_reason", length = 500)
    private String removalReason;
    @Column(name = "download_count", nullable = false)
    private long downloadCount;
    @Column(name = "demo_resource", nullable = false)
    private boolean demoResource;
    @Column(nullable = false)
    private boolean removed;

    public Resource() { }
    @PrePersist void onCreate() { if (uploadedAt == null) uploadedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Programme getProgramme() { return programme; }
    public Semester getSemester() { return semester; }
    public Subject getSubject() { return subject; }
    public ResourceType getResourceType() { return resourceType; }
    public FileFormat getFileFormat() { return fileFormat; }
    public String getDisplayFilename() { return displayFilename; }
    public String getStoredFilename() { return storedFilename; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public Integer getExamYear() { return examYear; }
    public ExamType getExamType() { return examType; }
    public String getExamSession() { return examSession; }
    public ModerationStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public AppUser getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public AppUser getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public AppUser getRemovedBy() { return removedBy; }
    public LocalDateTime getRemovedAt() { return removedAt; }
    public String getRemovalReason() { return removalReason; }
    public long getDownloadCount() { return downloadCount; }
    public boolean isDemoResource() { return demoResource; }
    public boolean isRemoved() { return removed; }

    public void setDetails(String title, String description, Programme programme, Semester semester, Subject subject,
                           ResourceType resourceType, FileFormat fileFormat, String displayFilename,
                           String storedFilename, long fileSizeBytes, Integer examYear, ExamType examType,
                           String examSession) {
        this.title = title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.programme = programme;
        this.semester = semester;
        this.subject = subject;
        this.resourceType = resourceType;
        this.fileFormat = fileFormat;
        this.displayFilename = displayFilename;
        if (storedFilename != null) this.storedFilename = storedFilename;
        if (fileSizeBytes >= 0) this.fileSizeBytes = fileSizeBytes;
        this.examYear = examYear;
        this.examType = examType;
        this.examSession = examSession == null || examSession.isBlank() ? null : examSession.trim();
        this.status = ModerationStatus.PENDING;
        this.rejectionReason = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
    }
    public void updateSampleContent(String title, String description, FileFormat fileFormat, String displayFilename,
                                    String storedFilename, long fileSizeBytes) {
        this.title = title.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.fileFormat = fileFormat;
        this.displayFilename = displayFilename;
        if (storedFilename != null) this.storedFilename = storedFilename;
        if (fileSizeBytes >= 0) this.fileSizeBytes = fileSizeBytes;
    }
    public void setUploader(AppUser user) { this.uploadedBy = user; }
    public void setDemoResource(boolean demoResource) { this.demoResource = demoResource; }
    public void approve(AppUser admin) {
        status = ModerationStatus.APPROVED; rejectionReason = null; reviewedBy = admin; reviewedAt = LocalDateTime.now();
    }
    public void reject(AppUser admin, String reason) {
        status = ModerationStatus.REJECTED; rejectionReason = reason; reviewedBy = admin; reviewedAt = LocalDateTime.now();
    }
    public void recordDownload() { downloadCount++; }
    public void markRemoved() { removed = true; }
    public void markRemovedByAdmin(AppUser admin, String reason) {
        removed = true;
        removedBy = admin;
        removedAt = LocalDateTime.now();
        removalReason = reason;
    }
}
