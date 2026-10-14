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
import edu.collegehub.DemoDataInitializer;
import edu.collegehub.catalog.ProgrammeRepository;
import edu.collegehub.catalog.CatalogueService;
import edu.collegehub.catalog.SemesterRepository;
import edu.collegehub.catalog.SubjectRepository;
import edu.collegehub.resources.ModerationStatus;
import edu.collegehub.resources.ExamType;
import edu.collegehub.resources.FileFormat;
import edu.collegehub.resources.Resource;
import edu.collegehub.resources.ResourceRepository;
import edu.collegehub.resources.ResourceType;
import edu.collegehub.resources.UploadStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired ProgrammeRepository programmes;
    @Autowired SemesterRepository semesters;
    @Autowired SubjectRepository subjects;
    @Autowired CatalogueService catalogue;
    @Autowired ResourceRepository resources;
    @Autowired UploadStorageService storage;
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
    void adminBootstrapIsHashedIdempotentAndDoesNotElevateStudentAccounts() {
        String email = "bootstrap" + System.nanoTime() + "@collegehub.test";
        new DemoDataInitializer(users, passwordEncoder, "  " + email.toUpperCase() + "  ",
                "BootstrapAdminPassword123!", "Fictional Bootstrap Admin").run(new DefaultApplicationArguments());
        AppUser created = users.findByEmailIgnoreCase(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(AccountRole.ADMIN, created.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("BootstrapAdminPassword123!", created.getPassword()));
        String originalHash = created.getPassword();

        new DemoDataInitializer(users, passwordEncoder, email, "DifferentPassword456!", "Another Name")
                .run(new DefaultApplicationArguments());
        org.junit.jupiter.api.Assertions.assertEquals(originalHash, users.findByEmailIgnoreCase(email).orElseThrow().getPassword());

        String studentEmail = "existing-student" + System.nanoTime() + "@collegehub.test";
        AppUser registeredStudent = users.save(new AppUser(studentEmail,
                passwordEncoder.encode("StudentTestPassword!"), "Fictional Existing Student", AccountRole.STUDENT));
        IllegalStateException collision = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new DemoDataInitializer(users, passwordEncoder, studentEmail, "AdminBootstrapPassword123!", "Admin")
                        .run(new DefaultApplicationArguments()));
        org.junit.jupiter.api.Assertions.assertTrue(collision.getMessage().contains("already belongs to a student account"));
        org.junit.jupiter.api.Assertions.assertEquals(AccountRole.STUDENT,
                users.findById(registeredStudent.getId()).orElseThrow().getRole());

        IllegalStateException incomplete = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new DemoDataInitializer(users, passwordEncoder, email + ".other", "", "Admin")
                        .run(new DefaultApplicationArguments()));
        org.junit.jupiter.api.Assertions.assertTrue(incomplete.getMessage().contains("Set both COLLEGEHUB_ADMIN_EMAIL"));
        IllegalStateException shortPassword = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new DemoDataInitializer(users, passwordEncoder, email + ".short", "short", "Admin")
                        .run(new DefaultApplicationArguments()));
        org.junit.jupiter.api.Assertions.assertTrue(shortPassword.getMessage().contains("at least 12 characters"));
    }

    @Test
    void adminSignsInNormallyAndCanOpenTheReviewQueue() throws Exception {
        var login = mvc.perform(post("/login").param("username", admin.getEmail())
                        .param("password", "AdminTestPassword!").with(csrf()))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session, "successful login should create a session");
        mvc.perform(get("/admin/uploads").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Review queue")));
    }

    @Test
    void publicPagesAndProtectedUploadRouteRenderCorrectly() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Select course")))
                .andExpect(content().string(containsString("/js/catalogue.js")));
        mvc.perform(get("/resources").param("allCourses", "true")).andExpect(status().isOk());
        mvc.perform(get("/question-papers").param("allCourses", "true")).andExpect(status().isOk());
        mvc.perform(get("/uploads/new")).andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/login")));
        mvc.perform(get("/uploads/new").with(user(student))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Accepted formats: PDF, PPTX, DOCX, JPG, PNG")));
        mvc.perform(post("/login").param("username", student.getEmail()).param("password", "StudentTestPassword!")
                        .param("next", "/uploads/new").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/uploads/new")));
        mvc.perform(get("/admin/uploads").with(user(student))).andExpect(status().isForbidden());
        createApprovedResource("BCA Java Programming notes", "An outline of Java classes and methods.", programmeId,
                semesterId, subjectId, ResourceType.NOTES, FileFormat.PDF, "java-programming-notes.pdf", null, null, null);
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
    void searchMatchesWordsAcrossResourceMetadataAndKeepsPresentationOutOfPapers() throws Exception {
        var biologyProgramme = programmes.findByCode("BSCBIO").orElseThrow();
        var biologySemester = semesters.findByProgrammeIdOrderByNumberAsc(biologyProgramme.getId()).get(0);
        var biologySubject = subjects.findByCode("BSCBIO-CELL-101").orElseThrow();
        createApprovedResource("Cell structure lecture slides", "Introduction to mitosis and membranes.",
                biologyProgramme.getId(), biologySemester.getId(), biologySubject.getId(), ResourceType.PRESENTATION,
                FileFormat.PPTX, "cell-biology-lecture.pptx", null, null, null);

        var secondSemester = semesters.findByProgrammeIdOrderByNumberAsc(programmes.findByCode("BCA").orElseThrow().getId()).get(1);
        var dbms = subjects.findByCode("BCA-DBMS-201").orElseThrow();
        createApprovedResource("Database internal exam paper", "Semester 2 previous question paper.", programmeId,
                secondSemester.getId(), dbms.getId(), ResourceType.QUESTION_PAPER, FileFormat.PDF,
                "dbms-internal-exam-2024.pdf", 2024, ExamType.MID_TERM, "Internal assessment");

        mvc.perform(get("/resources").param("allCourses", "true").param("q", "biology PPT"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Cell structure lecture slides")));
        mvc.perform(get("/question-papers").param("allCourses", "true").param("q", "internal exam"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Database internal exam paper")));
        mvc.perform(get("/question-papers").param("allCourses", "true").param("q", "semester 2 question paper"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Database internal exam paper")));
        mvc.perform(get("/question-papers").param("allCourses", "true").param("q", "2024"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Database internal exam paper")));
        mvc.perform(get("/question-papers").param("allCourses", "true").param("q", "biology PPT"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(containsString("Cell structure lecture slides"))));
    }

    @Test
    void subjectSuggestionsAreDatabaseBackedPartialAndDisambiguated() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals("java programming",
                subjects.findByCode("BCA-JAVA-101").orElseThrow().getNormalizedName());
        mvc.perform(get("/api/catalog/subjects/suggestions").param("q", "database"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Database Management Systems")))
                .andExpect(content().string(containsString("BCA"))).andExpect(content().string(containsString("Semester 2")));
        mvc.perform(get("/api/catalog/subjects/suggestions").param("q", "Statistics with R"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Statistics with R")));

        var bca = programmes.findByCode("BCA").orElseThrow();
        var bcom = programmes.findByCode("BCOM").orElseThrow();
        var bcaFirstSemester = semesters.findByProgrammeIdOrderByNumberAsc(bca.getId()).get(0);
        var existingJava = subjects.findByCode("BCA-JAVA-101").orElseThrow();
        var reusedJava = catalogue.resolveSubjectName(bca, bcaFirstSemester, "  jAvA   programming  ");
        org.junit.jupiter.api.Assertions.assertEquals(existingJava.getId(), reusedJava.getId());
        org.junit.jupiter.api.Assertions.assertEquals("Java Programming", reusedJava.getName());
        var bcaSemester = semesters.findByProgrammeIdOrderByNumberAsc(bca.getId()).get(2);
        var bcomSemester = semesters.findByProgrammeIdOrderByNumberAsc(bcom.getId()).get(0);
        var created = catalogue.resolveSubjectName(bca, bcaSemester, "  Applied   Mathematics  ");
        var duplicate = catalogue.resolveSubjectName(bca, bcaSemester, "applied mathematics");
        var similarOtherCourse = catalogue.resolveSubjectName(bcom, bcomSemester, "applied mathematics");
        org.junit.jupiter.api.Assertions.assertEquals(created.getId(), duplicate.getId());
        org.junit.jupiter.api.Assertions.assertEquals("Applied Mathematics", created.getName());
        org.junit.jupiter.api.Assertions.assertNotEquals(created.getId(), similarOtherCourse.getId());
        createApprovedResource("BCA applied mathematics notes", "BCA fixture", bca.getId(), bcaSemester.getId(),
                created.getId(), ResourceType.NOTES, FileFormat.PDF, "bca-math.pdf", null, null, null);
        createApprovedResource("B.Com applied mathematics notes", "B.Com fixture", bcom.getId(), bcomSemester.getId(),
                similarOtherCourse.getId(), ResourceType.NOTES, FileFormat.PDF, "bcom-math.pdf", null, null, null);

        mvc.perform(get("/api/catalog/subjects/suggestions").param("q", "math"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Applied Mathematics")))
                .andExpect(content().string(containsString("BCA"))).andExpect(content().string(containsString("B.Com")));
        mvc.perform(get("/resources").param("allCourses", "true").param("q", "Applied Mathematics")
                        .param("subjectId", created.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("BCA applied mathematics notes")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("B.Com applied mathematics notes"))));
        mvc.perform(get("/api/catalog/subjects/suggestions").param("q", "no-such-subject-term"))
                .andExpect(status().isOk()).andExpect(content().string("[]"));
    }

    @Test
    void paginationUsesOnlyRealApprovedUploadsAndHomeHasWorkingSearchInsteadOfSamples() throws Exception {
        for (int i = 0; i < 11; i++) {
            createApprovedResource("Java resource " + i, "Test content", programmeId, semesterId, subjectId,
                    ResourceType.NOTES, FileFormat.PDF, "java-resource-" + i + ".pdf", null, null, null);
        }
        mvc.perform(get("/resources").param("allCourses", "true").param("page", "1"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Page 2 of 2")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("concepts sample"))));
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Select course")))
                .andExpect(content().string(containsString("Browse resources")))
                .andExpect(content().string(containsString("Upload resource")))
                .andExpect(content().string(containsString("Search resources or subjects")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("home-search-panel"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("sample-stack"))));
    }

    @Test
    void legacyDemoRecordsAreHiddenFromBrowseDetailAndDownload() throws Exception {
        Resource demo = new Resource();
        demo.setDetails("Legacy demo resource", "Fictional example", programmes.findById(programmeId).orElseThrow(),
                semesters.findById(semesterId).orElseThrow(), subjects.findById(subjectId).orElseThrow(),
                ResourceType.NOTES, FileFormat.PDF, "legacy-demo.pdf", java.util.UUID.randomUUID() + ".pdf",
                1200, null, null, null);
        demo.setDemoResource(true);
        resources.save(demo);
        demo.approve(admin);
        resources.save(demo);

        mvc.perform(get("/resources").param("allCourses", "true").param("q", "Legacy demo resource"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("0 results")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Fictional example"))));
        mvc.perform(get("/resources/{id}", demo.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/resources/{id}/download", demo.getId())).andExpect(status().is3xxRedirection());
    }

    @Test
    void registrationAcademicPreferencesAreOptionalAndProgrammeSemestersComeFromDatabase() throws Exception {
        var bca = programmes.findByCode("BCA").orElseThrow();
        var bcom = programmes.findByCode("BCOM").orElseThrow();
        var bcaSemesters = semesters.findByProgrammeIdOrderByNumberAsc(bca.getId());
        var bcomSemesters = semesters.findByProgrammeIdOrderByNumberAsc(bcom.getId());
        org.junit.jupiter.api.Assertions.assertEquals(6, bcaSemesters.size());
        org.junit.jupiter.api.Assertions.assertEquals(6, bcomSemesters.size());

        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose programme (optional)")))
                .andExpect(content().string(containsString("Choose a programme first")))
                .andExpect(content().string(containsString("/js/catalogue.js")));
        mvc.perform(get("/api/catalog/programmes/{id}/semesters", bca.getId()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Semester 6")));
        mvc.perform(get("/api/catalog/programmes/{id}/semesters", bcom.getId()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Semester 6")));

        String blankEmail = "blank" + System.nanoTime() + "@collegehub.test";
        mvc.perform(post("/register").with(csrf()).param("fullName", "Fictional Blank Preference Student")
                        .param("email", blankEmail).param("password", "StudentTestPassword!"))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/login")));
        var noPreferences = users.findByEmailIgnoreCase(blankEmail).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNull(noPreferences.getPreferredProgramme());
        org.junit.jupiter.api.Assertions.assertNull(noPreferences.getCurrentSemester());

        String selectedEmail = "selected" + System.nanoTime() + "@collegehub.test";
        mvc.perform(post("/register").with(csrf()).param("fullName", "Fictional Selected Preference Student")
                        .param("email", selectedEmail).param("password", "StudentTestPassword!")
                        .param("programmeId", bca.getId().toString()).param("semesterId", bcaSemesters.get(1).getId().toString()))
                .andExpect(status().is3xxRedirection());
        var selected = users.findByEmailIgnoreCase(selectedEmail).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(bca.getId(), selected.getPreferredProgramme().getId());
        org.junit.jupiter.api.Assertions.assertEquals(bcaSemesters.get(1).getId(), selected.getCurrentSemester().getId());

        mvc.perform(post("/register").with(csrf()).param("fullName", "Fictional Programme Only Student")
                        .param("email", "programme-only" + System.nanoTime() + "@collegehub.test")
                        .param("password", "StudentTestPassword!").param("programmeId", bca.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose a current semester for this programme")))
                .andExpect(content().string(containsString("value=\"" + bca.getId() + "\" selected")));

        mvc.perform(post("/register").with(csrf()).param("fullName", "Fictional Semester Only Student")
                        .param("email", "semester-only" + System.nanoTime() + "@collegehub.test")
                        .param("password", "StudentTestPassword!").param("semesterId", bcaSemesters.get(0).getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose the programme that this semester belongs to")))
                .andExpect(content().string(containsString("value=\"" + bcaSemesters.get(0).getId() + "\" selected")));

        mvc.perform(post("/register").with(csrf()).param("fullName", "Fictional Mismatched Preference Student")
                        .param("email", "mismatch" + System.nanoTime() + "@collegehub.test")
                        .param("password", "StudentTestPassword!").param("programmeId", bcom.getId().toString())
                        .param("semesterId", bcaSemesters.get(0).getId().toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("belonging to the selected programme")));
    }

    private Resource createApprovedResource(String title, String description, Long programmeId, Long semesterId,
                                            Long subjectId, ResourceType type, FileFormat format, String filename,
                                            Integer year, ExamType examType, String session) {
        Resource resource = new Resource();
        resource.setDetails(title, description, programmes.findById(programmeId).orElseThrow(),
                semesters.findById(semesterId).orElseThrow(), subjects.findById(subjectId).orElseThrow(), type,
                format, filename, java.util.UUID.randomUUID() + "." + format.getExtension(), 1200,
                year, examType, session);
        resource.setUploader(student);
        resources.save(resource);
        resource.approve(admin);
        return resources.save(resource);
    }

    private Resource uploadPendingPdf(String title, String filename) throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", filename, "application/pdf",
                "%PDF-1.4\nFictional moderation test\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        mvc.perform(multipart("/uploads/new").file(pdf).param("title", title)
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectName", "Java Programming").param("resourceType", "NOTES")
                        .param("description", "Fictional test upload for the removal workflow")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        return resources.findByUploadedByAndRemovedFalseOrderByUploadedAtDesc(student).stream()
                .filter(resource -> resource.getTitle().equals(title)).findFirst().orElseThrow();
    }

    @Test
    void adminCanRemovePendingUploadAndUploaderSeesRemovalAudit() throws Exception {
        String title = "Pending removal test " + System.nanoTime();
        Resource pending = uploadPendingPdf(title, "pending-removal.pdf");
        var filePath = storage.pathFor(pending.getStoredFilename());
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(filePath));

        mvc.perform(get("/admin/uploads").with(user(admin)))
                .andExpect(status().isOk()).andExpect(content().string(containsString(title)))
                .andExpect(content().string(containsString("Remove from platform")));
        mvc.perform(post("/admin/uploads/{id}/remove", pending.getId()).param("reason", " x ")
                        .with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("moderationError", containsString("between 3 and 500")));
        org.junit.jupiter.api.Assertions.assertFalse(resources.findById(pending.getId()).orElseThrow().isRemoved());
        mvc.perform(post("/admin/uploads/{id}/remove", pending.getId()).param("reason", "Outdated and misleading notes.")
                        .with(user(student)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/uploads/{id}/remove", pending.getId()).param("reason", "Outdated and misleading notes.")
                        .with(user(admin)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/admin/uploads/{id}/remove", pending.getId()).param("reason", "Outdated and misleading notes.")
                        .with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("moderationMessage", containsString("removed from public access")));
        Resource removed = resources.findById(pending.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(removed.isRemoved());
        org.junit.jupiter.api.Assertions.assertEquals(admin.getId(), removed.getRemovedBy().getId());
        org.junit.jupiter.api.Assertions.assertNotNull(removed.getRemovedAt());
        org.junit.jupiter.api.Assertions.assertEquals("Outdated and misleading notes.", removed.getRemovalReason());
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(filePath));
        mvc.perform(get("/admin/uploads").with(user(admin)))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(containsString(title))));
        mvc.perform(get("/resources").param("allCourses", "true").param("q", title))
                .andExpect(status().isOk()).andExpect(content().string(containsString("No resources match this search")));
        mvc.perform(get("/resources/{id}", pending.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/resources/{id}/download", pending.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/my-uploads").with(user(student))).andExpect(status().isOk())
                .andExpect(content().string(containsString(title))).andExpect(content().string(containsString("REMOVED")))
                .andExpect(content().string(containsString("Outdated and misleading notes.")));
    }

    @Test
    void adminCanRemoveApprovedUploadFromDetailAndDisablePublicDownload() throws Exception {
        String title = "Approved removal test " + System.nanoTime();
        Resource approved = uploadPendingPdf(title, "approved-removal.pdf");
        var filePath = storage.pathFor(approved.getStoredFilename());
        mvc.perform(post("/admin/uploads/{id}/approve", approved.getId()).with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/resources/{id}", approved.getId()).with(user(admin)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("admin-removal-panel")))
                .andExpect(content().string(containsString("Reason for removal")));
        mvc.perform(get("/resources").param("allCourses", "true").param("q", title))
                .andExpect(status().isOk()).andExpect(content().string(containsString(title)));

        mvc.perform(post("/admin/uploads/{id}/remove", approved.getId()).param("reason", "This file contains private student data.")
                        .with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Resource removed = resources.findById(approved.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ModerationStatus.APPROVED, removed.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(removed.isRemoved());
        org.junit.jupiter.api.Assertions.assertEquals(admin.getId(), removed.getRemovedBy().getId());
        org.junit.jupiter.api.Assertions.assertEquals("This file contains private student data.", removed.getRemovalReason());
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(filePath));
        mvc.perform(get("/resources").param("allCourses", "true").param("q", title))
                .andExpect(status().isOk()).andExpect(content().string(containsString("No resources match this search")));
        mvc.perform(get("/resources/{id}", approved.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/resources/{id}/download", approved.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/my-uploads").with(user(student))).andExpect(status().isOk())
                .andExpect(content().string(containsString("This file contains private student data.")));
    }

    @Test
    void studentUploadStaysPrivateUntilAdminApprovesThenDownloads() throws Exception {
        String title = "Workflow test " + System.nanoTime();
        MockMultipartFile pdf = new MockMultipartFile("file", "practice.pdf", "application/pdf",
                "%PDF-1.4\nFictional workflow test\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        mvc.perform(multipart("/uploads/new").file(pdf)
                        .param("title", title).param("programmeId", programmeId.toString())
                        .param("semesterId", semesterId.toString()).param("subjectName", "Data Structures and Algorithms")
                        .param("resourceType", "NOTES").param("description", "A fictional test upload")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/my-uploads")));
        Resource uploaded = resources.findByUploadedByAndRemovedFalseOrderByUploadedAtDesc(student).stream()
                .filter(r -> r.getTitle().equals(title)).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ModerationStatus.PENDING, uploaded.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Data Structures and Algorithms", uploaded.getSubject().getName());
        mvc.perform(get("/api/catalog/subjects/suggestions").param("q", "data structures")
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Data Structures and Algorithms")));
        mvc.perform(get("/resources/{id}", uploaded.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/resources/{id}/download", uploaded.getId())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/uploads").with(user(admin)))
                .andExpect(status().isOk()).andExpect(content().string(containsString(title)))
                .andExpect(content().string(containsString("Review file")));
        mvc.perform(get("/admin/uploads/{id}/file", uploaded.getId()).with(user(student)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/uploads/{id}/file", uploaded.getId()).with(user(admin)))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
        mvc.perform(post("/admin/uploads/{id}/approve", uploaded.getId()).with(user(student)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/uploads/{id}/approve", uploaded.getId()).with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", containsString("/admin/uploads")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("moderationMessage", containsString("approved")));
        mvc.perform(get("/resources/{id}", uploaded.getId())).andExpect(status().isOk());
        mvc.perform(get("/resources/{id}/download", uploaded.getId())).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/my-uploads").with(user(student))).andExpect(status().isOk())
                .andExpect(content().string(containsString("APPROVED")));
        mvc.perform(get("/my-uploads/{id}/edit", uploaded.getId()).with(user(otherStudent)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPdfContentIsRejectedAndOtherExamRequiresExplanation() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile("file", "not-really.pdf", "application/pdf", "plain text".getBytes());
        mvc.perform(multipart("/uploads/new").file(fakePdf).param("title", "Fictional invalid upload")
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectName", "Java Programming").param("resourceType", "NOTES")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("contents do not match")));
        MockMultipartFile pdf = new MockMultipartFile("file", "paper.pdf", "application/pdf",
                "%PDF-1.4\nFictional paper\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        mvc.perform(multipart("/uploads/new").file(pdf).param("title", "Fictional other exam paper")
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectName", "Java Programming").param("resourceType", "QUESTION_PAPER")
                        .param("examYear", "2025").param("examType", "OTHER")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Describe what kind of exam")));
        Long unrelatedSemester = semesters.findByProgrammeIdOrderByNumberAsc(
                programmes.findByCode("BCOM").orElseThrow().getId()).get(0).getId();
        mvc.perform(multipart("/uploads/new").file(pdf).param("title", "Mismatched course and subject")
                        .param("programmeId", programmeId.toString()).param("semesterId", unrelatedSemester.toString())
                        .param("subjectName", "Any subject name").param("resourceType", "NOTES")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("semester belonging to the selected course")));
    }

    @Test
    void rejectionReasonAppearsAndResubmissionReturnsToPending() throws Exception {
        String title = "Fictional rejection workflow " + System.nanoTime();
        MockMultipartFile pdf = new MockMultipartFile("file", "revision.pdf", "application/pdf",
                "%PDF-1.4\nFictional revision example\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        mvc.perform(multipart("/uploads/new").file(pdf).param("title", title)
                        .param("programmeId", programmeId.toString()).param("semesterId", semesterId.toString())
                        .param("subjectName", "Java Programming").param("resourceType", "NOTES")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Resource uploaded = resources.findByUploadedByAndRemovedFalseOrderByUploadedAtDesc(student).stream()
                .filter(r -> r.getTitle().equals(title)).findFirst().orElseThrow();

        mvc.perform(post("/admin/uploads/{id}/reject", uploaded.getId()).param("reason", " x ")
                        .with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("moderationError", containsString("between 3 and 500")));
        org.junit.jupiter.api.Assertions.assertEquals(ModerationStatus.PENDING,
                resources.findById(uploaded.getId()).orElseThrow().getStatus());
        mvc.perform(post("/admin/uploads/{id}/reject", uploaded.getId()).with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("moderationError", containsString("between 3 and 500")));

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
                        .param("subjectName", "Java Programming").param("resourceType", "NOTES")
                        .param("permissionConfirmed", "true").with(user(student)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(ModerationStatus.PENDING,
                resources.findById(uploaded.getId()).orElseThrow().getStatus());
        mvc.perform(get("/resources/{id}", uploaded.getId())).andExpect(status().is3xxRedirection());
    }
}
