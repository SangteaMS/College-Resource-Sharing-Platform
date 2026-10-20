package edu.collegehub;

import edu.collegehub.accounts.AccountRole;
import edu.collegehub.accounts.AppUser;
import edu.collegehub.accounts.AppUserRepository;
import edu.collegehub.catalog.SemesterRepository;
import edu.collegehub.catalog.SubjectRepository;
import edu.collegehub.catalog.ProgrammeRepository;
import edu.collegehub.resources.ModerationStatus;
import edu.collegehub.resources.Resource;
import edu.collegehub.resources.ResourceRepository;
import edu.collegehub.resources.ResourceType;
import edu.collegehub.resources.UploadStorageService;
import edu.collegehub.resources.FileFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:collegehub-local-seed-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "collegehub.storage.location=./target/local-seed-test-uploads",
        "collegehub.seed.admin-email=",
        "collegehub.seed.admin-password="
})
class LocalDemoCatalogueInitializerTest {
    @Autowired MockMvc mvc;
    @Autowired ProgrammeRepository programmes;
    @Autowired SemesterRepository semesters;
    @Autowired SubjectRepository subjects;
    @Autowired AppUserRepository users;
    @Autowired ResourceRepository resources;
    @Autowired LocalDemoCatalogueInitializer initializer;
    @Autowired UploadStorageService storage;

    @Test
    @Transactional
    void seedsVisibleDownloadsAndIsIdempotent() throws Exception {
        assertEquals(17, programmes.count());
        assertEquals(32, resources.count());
        assertTrue(resources.findAll().stream().allMatch(resource -> resource.getStatus() == ModerationStatus.APPROVED));
        assertTrue(resources.findAll().stream().noneMatch(Resource::isDemoResource));

        Resource example = resources.findAll().stream()
                .filter(resource -> resource.getTitle().equals("Sample: Java Programming Quick Notes"))
                .findFirst().orElseThrow();
        assertNotNull(example.getProgramme());
        assertNotNull(example.getSemester());
        assertNotNull(example.getSubject());
        var file = storage.pathFor(example.getStoredFilename());
        assertTrue(Files.exists(file));
        byte[] samplePdf = Files.readAllBytes(file);
        assertTrue(samplePdf.length > 100);
        assertTrue(new String(samplePdf, java.nio.charset.StandardCharsets.US_ASCII)
                .contains("Fictional sample material. Not an official college document."));

        mvc.perform(get("/resources").param("q", "Java Programming Quick Notes").param("allCourses", "true"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Java Programming Quick Notes")));
        mvc.perform(get("/resources/{id}", example.getId()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Java Programming Quick Notes")));
        mvc.perform(get("/resources/{id}/download", example.getId()))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("java-programming-quick-notes.pdf")))
                .andExpect(content().bytes(Files.readAllBytes(file)));

        var bca = programmes.findByCode("BCA").orElseThrow();
        var firstSemester = semesters.findByProgrammeIdOrderByNumberAsc(bca.getId()).get(0);
        var javaSubject = subjects.findByCode("BCA-JAVA-101").orElseThrow();
        var student = users.saveAndFlush(new AppUser("preservation-check@example.invalid", "not-a-login-hash",
                "Fictional Student", AccountRole.STUDENT));
        var existingFile = storage.store(new UploadStorageService.ValidatedUpload("existing-student-upload.pdf",
                FileFormat.PDF, "%PDF-1.4\nexisting fictional student upload".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        Resource existingUpload = new Resource();
        existingUpload.setDetails("Existing student upload", "This row simulates a real pending student upload.", bca,
                firstSemester, javaSubject, ResourceType.NOTES, existingFile.format(), existingFile.displayFilename(),
                existingFile.storedFilename(), existingFile.size(), null, null, null);
        existingUpload.setUploader(student);
        existingUpload = resources.saveAndFlush(existingUpload);
        Long existingUploadId = existingUpload.getId();

        example.setDetails("Demo: Java Programming Quick Notes", example.getDescription(), example.getProgramme(),
                example.getSemester(), example.getSubject(), example.getResourceType(), example.getFileFormat(),
                example.getDisplayFilename(), example.getStoredFilename(), example.getFileSizeBytes(), null, null, null);
        example.approve(null);
        var previousReviewTime = example.getReviewedAt();
        resources.saveAndFlush(example);

        long beforeRestart = resources.count();
        initializer.run(new DefaultApplicationArguments());
        assertEquals(beforeRestart, resources.count());
        assertEquals(17, programmes.count());
        Resource renamedSample = resources.findById(example.getId()).orElseThrow();
        assertEquals("Sample: Java Programming Quick Notes", renamedSample.getTitle());
        assertEquals(ModerationStatus.APPROVED, renamedSample.getStatus());
        assertEquals(previousReviewTime, renamedSample.getReviewedAt());
        assertTrue(new String(Files.readAllBytes(storage.pathFor(renamedSample.getStoredFilename())),
                java.nio.charset.StandardCharsets.US_ASCII)
                .contains("Fictional sample material. Not an official college document."));
        initializer.run(new DefaultApplicationArguments());
        assertEquals(beforeRestart, resources.count());
        Resource preservedUpload = resources.findById(existingUploadId).orElseThrow();
        assertEquals(ModerationStatus.PENDING, preservedUpload.getStatus());
        assertEquals("Existing student upload", preservedUpload.getTitle());
        assertEquals(existingFile.storedFilename(), preservedUpload.getStoredFilename());
        assertEquals(student.getId(), preservedUpload.getUploadedBy().getId());
        assertTrue(Files.exists(existingFile.path()));
        assertFalse(resources.findAll().stream().anyMatch(resource -> resource.getTitle().equals("Sample: Java Programming Quick Notes")
                && resource.getStatus() != ModerationStatus.APPROVED));

        String home = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int firstGridStart = home.indexOf("<div class=\"programme-grid\">");
        int firstGridEnd = home.indexOf("</div>", firstGridStart);
        String firstGrid = home.substring(firstGridStart, firstGridEnd);
        assertEquals(10, firstGrid.split("class=\"programme-card\"", -1).length - 1);
        assertTrue(home.contains("<details class=\"programme-more\">"));
        assertTrue(home.contains("View all courses"));
        assertTrue(home.contains("Show fewer"));
        String extraCourses = home.substring(home.indexOf("<details class=\"programme-more\">"), home.indexOf("</details>"));
        assertEquals(7, extraCourses.split("class=\"programme-card\"", -1).length - 1);
        String courseSelector = mvc.perform(get("/resources").param("allCourses", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int selectorStart = courseSelector.indexOf("id=\"filter-programme\"");
        int selectorEnd = courseSelector.indexOf("</select>", selectorStart);
        assertEquals(17, courseSelector.substring(selectorStart, selectorEnd).split("<option value=\"\\d+\"", -1).length - 1);
        String styles = mvc.perform(get("/css/styles.css")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(styles.contains(".programme-more[open] .show-all-courses"));
        assertTrue(styles.contains(".programme-more:not([open]) .show-fewer-courses"));

        String listing = mvc.perform(get("/resources").param("q", "Java Programming Quick Notes").param("allCourses", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(listing.contains(">Sample: Java Programming Quick Notes</a>"));
        assertFalse(listing.contains("Demo:"));
        assertFalse(listing.contains("Illustrative demo material"));
        assertTrue(listing.contains("Fictional sample material. Not an official college document."));
        assertTrue(listing.contains("Practice papers are fictional examples, not authentic university papers."));

        String detail = mvc.perform(get("/resources/{id}", example.getId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(detail.contains(">Sample: Java Programming Quick Notes</h1>"));
        assertFalse(detail.contains("Demo:"));
        assertFalse(detail.contains("Illustrative demo material"));
        assertTrue(detail.contains("Fictional sample material. Not an official college document."));
    }
}
