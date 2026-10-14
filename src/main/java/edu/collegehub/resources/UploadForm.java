package edu.collegehub.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class UploadForm {
    @NotBlank @Size(max = 180)
    private String title;
    @NotNull
    private Long programmeId;
    @NotNull
    private Long semesterId;
    @NotBlank @Size(max = 150)
    private String subjectName;
    @NotNull
    private ResourceType resourceType;
    @Size(max = 500)
    private String description;
    private MultipartFile file;
    @Min(1900) @Max(2200)
    private Integer examYear;
    private ExamType examType;
    @Size(max = 100)
    private String examSession;
    private boolean permissionConfirmed;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getProgrammeId() { return programmeId; }
    public void setProgrammeId(Long programmeId) { this.programmeId = programmeId; }
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName == null ? null : subjectName.trim(); }
    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    public Integer getExamYear() { return examYear; }
    public void setExamYear(Integer examYear) { this.examYear = examYear; }
    public ExamType getExamType() { return examType; }
    public void setExamType(ExamType examType) { this.examType = examType; }
    public String getExamSession() { return examSession; }
    public void setExamSession(String examSession) { this.examSession = examSession; }
    public boolean isPermissionConfirmed() { return permissionConfirmed; }
    public void setPermissionConfirmed(boolean permissionConfirmed) { this.permissionConfirmed = permissionConfirmed; }
}
