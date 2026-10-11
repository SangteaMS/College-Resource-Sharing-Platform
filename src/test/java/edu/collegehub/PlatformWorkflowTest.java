package edu.collegehub;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.collegehub.accounts.AccountRole;
import edu.collegehub.accounts.AppUser;
import edu.collegehub.accounts.AppUserRepository;
import edu.collegehub.catalog.ProgrammeRepository;
import edu.collegehub.catalog.SemesterRepository;
import edu.collegehub.catalog.SubjectRepository;
import edu.collegehub.resources.ModerationStatus;
import edu.collegehub.resources.Resource;
import edu.collegehub.resources.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired ProgrammeRepository programmes;
    @Autowired SemesterRepository semesters;
    @Autowired SubjectRepository subjects;
    @Autowired ResourceRepository resources;
    @Autowired PasswordEncoder passwordEncoder;

    private AppUser student;
    private AppUser otherStudent;
    private AppUser admin;
    private Long programmeId;
    private Long semesterId;
    private Long subjectId;

    @BeforeEach
    void setUp() {
        student = users.save(new AppUser("student" + System.nanoTime() + "@collegehub.test",
                passwordEncoder.encode("StudentTestPassword!"), "Fictional Test Student", AccountRole.STUDENT));
        otherStudent = users.save(new AppUser("other" + System.nanoTime() + "@collegehub.test",
                passwordEncoder.encode("OtherTestPassword!"), "Fictional Other Student", AccountRole.STUDENT));
        admin = users.save(new AppUser("admin" + System.nanoTime() + "@collegehub.test",
                passwordEncoder.encode("AdminTestPassword!"), "Fictional Test Admin", AccountRole.ADMIN));
        var programme = programmes.findByCode("BCA").orElseThrow();
        var semester = semesters.findByProgrammeIdOrderByNumberAsc(programme.getId()).get(0);
        var subject = subjects.findByCode("BCA-JAVA-101").orElseThrow();
        programmeId = programme.getId(); semesterId = semester.getId(); subjectId = subject.getId();
    }

    @Test
    void publicPagesAndProtectedUploadRouteRenderCorrectly() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Select course")));
        mvc.perform(get("/resources").param("allCourses", "true")).andExpect(status().isOk());
        mvc.perform(get("/question-papers").param("allCourses", "true")).andExpect(status().isOk());
        mvc.perform(get("/uploads/new")).andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/login")));
        mvc.perform(get("/uploads/new").with(user(student))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Accepted formats: PDF, PPTX, DOCX, JPG, PNG")));
        mvc.perform(post("/login").param("username", student.getEmail()).param("password", "StudentTestPassword!")
                        .param("next", "/uploads/new").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/uploads/new")));
        mvc.perform(get("/admin/uploads").with(user(student))).andExpect(status().isForbidden());
        mvc.perform(get("/resources").param("programmeId", programmeId.toString())
                        .param("semesterId", semesterId.toString()).param("subjectId", subjectId.toString())
                        .param("q", "BCA-JAVA-101").param("sort", "most-downloaded"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("BCA Java Programming")));
        mvc.perform(get("/resources").param("allCourses", "true").param("page", "1"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Page 2 of 2")));
        mvc.perform(get("/api/catalog/programmes/{id}/semesters", programmeId))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Semester 1")));
        mvc.perform(get("/api/catalog/semesters/{id}/subjects", semesterId))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Java Programming")));
    }

    @Test
    void studentUploadStaysPrivateUntilAdminApprovesThenDownloads() throws Exception {
        String title = "Workflow test " + System.nanoTime();
        MockMultipartFile pdf = new MockMultipartFile("file", "practice.pdf", "application/pdf",
                "%PDF-1.4\nFictional workflow test\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        mvc.perform(multipart("/uploads/new").file(pdf)
                        .param("title", title).param("programmeId", programmeId.toString())
                        .param("semesterId", semesterId.toString()).param("subjectId", subjectId.toString())
                        .param("resourceType", "NOTES").param("description", "A fictional test upload")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/my-uploads")));
        Resource uploaded = resources.findByUploadedByAndRemovedFalseOrderByUploadedAtDesc(student).stream()
                .filter(r -> r.getTitle().equals(title)).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ModerationStatus.PENDING, uploaded.getStatus());
        mvc.perform(get("/resources/{id}", uploaded.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/resources/{id}/download", uploaded.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/uploads/{id}/approve", uploaded.getId()).with(user(student)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/uploads/{id}/approve", uploaded.getId()).with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/admin/uploads")));
        mvc.perform(get("/resources/{id}", uploaded.getId())).andExpect(status().isOk());
        mvc.perform(get("/resources/{id}/download", uploaded.getId())).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/my-uploads/{id}/edit", uploaded.getId()).with(user(otherStudent)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPdfContentIsRejectedAndOtherExamRequiresExplanation() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile("file", "not-really.pdf", "application/pdf", "plain text".getBytes());
        mvc.perform(multipart("/uploads/new").file(fakePdf).param("title", "Fictional invalid upload")
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectId", subjectId.toString()).param("resourceType", "NOTES")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("contents do not match")));
        MockMultipartFile pdf = new MockMultipartFile("file", "paper.pdf", "application/pdf",
                "%PDF-1.4\nFictional paper\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        mvc.perform(multipart("/uploads/new").file(pdf).param("title", "Fictional other exam paper")
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectId", subjectId.toString()).param("resourceType", "QUESTION_PAPER")
                        .param("examYear", "2025").param("examType", "OTHER")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Describe what kind of exam")));
        Long unrelatedSubject = subjects.findByCode("BCOM-ACC-101").orElseThrow().getId();
        mvc.perform(multipart("/uploads/new").file(pdf).param("title", "Mismatched course and subject")
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectId", unrelatedSubject.toString()).param("resourceType", "NOTES")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("subject belonging to the selected programme and semester")));
    }

    @Test
    void rejectionReasonAppearsAndResubmissionReturnsToPending() throws Exception {
        String title = "Fictional rejection workflow " + System.nanoTime();
        MockMultipartFile pdf = new MockMultipartFile("file", "revision.pdf", "application/pdf",
                "%PDF-1.4\nFictional revision example\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        mvc.perform(multipart("/uploads/new").file(pdf).param("title", title)
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectId", subjectId.toString()).param("resourceType", "NOTES")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Resource uploaded = resources.findByUploadedByAndRemovedFalseOrderByUploadedAtDesc(student).stream()
                .filter(r -> r.getTitle().equals(title)).findFirst().orElseThrow();

        mvc.perform(post("/admin/uploads/{id}/reject", uploaded.getId()).param("reason", "Add a clearer title.")
                        .with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Resource rejected = resources.findById(uploaded.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ModerationStatus.REJECTED, rejected.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Add a clearer title.", rejected.getRejectionReason());
        mvc.perform(get("/my-uploads").with(user(student))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Add a clearer title.")));

        mvc.perform(post("/my-uploads/{id}/edit", uploaded.getId()).param("title", title + " revised")
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectId", subjectId.toString()).param("resourceType", "NOTES")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(ModerationStatus.PENDING,
                resources.findById(uploaded.getId()).orElseThrow().getStatus());
        mvc.perform(get("/resources/{id}", uploaded.getId())).andExpect(status().is3xxRedirection());
    }
}
