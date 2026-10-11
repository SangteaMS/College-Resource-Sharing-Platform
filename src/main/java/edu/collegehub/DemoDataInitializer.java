package edu.collegehub;

import java.util.List;

import edu.collegehub.accounts.AccountRole;
import edu.collegehub.accounts.AppUser;
import edu.collegehub.accounts.AppUserRepository;
import edu.collegehub.catalog.ProgrammeRepository;
import edu.collegehub.catalog.SemesterRepository;
import edu.collegehub.catalog.SubjectRepository;
import edu.collegehub.resources.ExamType;
import edu.collegehub.resources.ModerationStatus;
import edu.collegehub.resources.ResourceRepository;
import edu.collegehub.resources.ResourceService;
import edu.collegehub.resources.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {
    private final ProgrammeRepository programmes;
    private final SemesterRepository semesters;
    private final SubjectRepository subjects;
    private final ResourceRepository resources;
    private final ResourceService resourceService;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public DemoDataInitializer(ProgrammeRepository programmes, SemesterRepository semesters,
                               SubjectRepository subjects, ResourceRepository resources,
                               ResourceService resourceService, AppUserRepository users,
                               PasswordEncoder passwordEncoder,
                               @Value("${collegehub.seed.admin-email:}") String adminEmail,
                               @Value("${collegehub.seed.admin-password:}") String adminPassword,
                               @Value("${collegehub.seed.admin-name:Demo Administrator}") String adminName) {
        this.programmes = programmes; this.semesters = semesters; this.subjects = subjects;
        this.resources = resources; this.resourceService = resourceService; this.users = users;
        this.passwordEncoder = passwordEncoder; this.adminEmail = adminEmail; this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedSamples();
    }

    @Transactional
    void seedAdmin() {
        if (adminEmail.isBlank() || adminPassword.isBlank() || users.existsByEmailIgnoreCase(adminEmail)) return;
        if (adminPassword.length() < 12) {
            throw new IllegalStateException("COLLEGEHUB_ADMIN_PASSWORD must be at least 12 characters when creating the first admin.");
        }
        users.save(new AppUser(adminEmail, passwordEncoder.encode(adminPassword), adminName, AccountRole.ADMIN));
    }

    void seedSamples() {
        List<Sample> samples = List.of(
                new Sample("BCA", 1, "BCA-JAVA-101", ResourceType.NOTES, null, null, "BCA Java Programming — concepts sample",
                        "Fictional study notes. A class describes a kind of object; methods describe behavior. This sample is an original project demonstration."),
                new Sample("BCOM", 1, "BCOM-ACC-101", ResourceType.STUDY_GUIDE, null, null, "B.Com Financial Accounting — study guide sample",
                        "Fictional study guide. Assets are resources controlled by a business; liabilities are obligations. This example is not an official course document."),
                new Sample("BBA", 1, "BBA-MGMT-101", ResourceType.PRACTICE_MATERIAL, null, null, "BBA Principles of Management — practice sample",
                        "Fictional practice material. Give one example of planning, organizing, leading, and controlling in a student club."),
                new Sample("BAPOL", 1, "BAPOL-POL-101", ResourceType.NOTES, null, null, "Political Theory — reading notes sample",
                        "Fictional reading notes. Compare two ways a political theorist might describe justice. This is a student project example, not a syllabus."),
                new Sample("BSCBIO", 1, "BSCBIO-CELL-101", ResourceType.LAB_PRACTICAL_MATERIAL, null, null, "Cell Biology — practical checklist sample",
                        "Fictional practical checklist. Record the sample label, magnification, observation, and date in a lab notebook."),
                new Sample("BCA", 1, "BCA-JAVA-101", ResourceType.QUESTION_PAPER, ExamType.MID_TERM, "Fictional demo session", "BCA Java Programming — 2025 Mid-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Explain the difference between a class and an object.\n2. Write a Java method that returns the larger of two integers.\n3. Describe how a for-loop works."),
                new Sample("BCA", 2, "BCA-DBMS-201", ResourceType.QUESTION_PAPER, ExamType.END_TERM, "Fictional demo session", "BCA Database Management Systems — 2025 End-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Explain primary and foreign keys.\n2. Write a query to select students in semester 2, ordered by name.\n3. Describe normalization in your own words."),
                new Sample("BCOM", 1, "BCOM-ACC-101", ResourceType.QUESTION_PAPER, ExamType.MID_TERM, "Fictional demo session", "B.Com Financial Accounting — 2025 Mid-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Define an asset and a liability.\n2. Explain the accounting equation.\n3. Give a fictional journal entry for cash received from a customer."),
                new Sample("BCOM", 2, "BCOM-ECO-201", ResourceType.QUESTION_PAPER, ExamType.END_TERM, "Fictional demo session", "B.Com Business Economics — 2025 End-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Explain demand using an example.\n2. What can shift a supply curve?\n3. Describe one limitation of a simple market model."),
                new Sample("BBA", 1, "BBA-MGMT-101", ResourceType.QUESTION_PAPER, ExamType.MID_TERM, "Fictional demo session", "BBA Principles of Management — 2025 Mid-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Give an example of planning.\n2. Describe two leadership approaches.\n3. Why might an organization review its goals?"),
                new Sample("BBA", 2, "BBA-MKT-201", ResourceType.QUESTION_PAPER, ExamType.END_TERM, "Fictional demo session", "BBA Marketing Management — 2025 End-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Describe a target audience.\n2. Name the four parts of a basic marketing mix.\n3. Give an example of responsible product research."),
                new Sample("BAPOL", 1, "BAPOL-POL-101", ResourceType.QUESTION_PAPER, ExamType.MID_TERM, "Fictional demo session", "B.A. Political Science Political Theory — 2025 Mid-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Explain one understanding of political authority.\n2. Compare liberty and equality.\n3. Give an example of a political argument."),
                new Sample("BAPOL", 2, "BAPOL-GOV-201", ResourceType.QUESTION_PAPER, ExamType.END_TERM, "Fictional demo session", "B.A. Political Science Indian Government and Politics — 2025 End-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Describe the purpose of a constitution.\n2. Explain how representation works.\n3. Distinguish local and national government."),
                new Sample("BSCBIO", 1, "BSCBIO-CELL-101", ResourceType.QUESTION_PAPER, ExamType.MID_TERM, "Fictional demo session", "B.Sc. Biotechnology Cell Biology — 2025 Mid-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Name two cell structures and their functions.\n2. Explain why microscopes use magnification.\n3. Describe one safe lab practice."),
                new Sample("BSCBIO", 2, "BSCBIO-GEN-201", ResourceType.QUESTION_PAPER, ExamType.END_TERM, "Fictional demo session", "B.Sc. Biotechnology Genetics — 2025 End-term sample paper",
                        "FICTIONAL PRACTICE PAPER — NOT AN OFFICIAL EXAM\n\n1. Define a gene in introductory terms.\n2. Explain genotype and phenotype.\n3. Use a simple cross to illustrate an inherited trait.")
        );

        for (Sample sample : samples) {
            if (resources.existsByDemoResourceTrueAndTitleIgnoreCase(sample.title())) continue;
            var programme = programmes.findByCode(sample.programmeCode()).orElseThrow();
            var semester = semesters.findByProgrammeIdOrderByNumberAsc(programme.getId()).stream()
                    .filter(option -> option.getNumber() == sample.semester()).findFirst().orElseThrow();
            var subject = subjects.findByCode(sample.subjectCode()).orElseThrow();
            resourceService.createDemo(programme, semester, subject, sample.type(),
                    sample.examType() == null ? null : 2025, sample.examType(), sample.session(), sample.title(),
                    sample.examType() == null ? "Fictional, original project demonstration. Not an official curriculum document." :
                            "Fictional original practice paper created for this project demo. Not an official exam and not faculty-verified.",
                    sample.text());
        }
        var bca = programmes.findByCode("BCA").orElseThrow();
        var thirdSemester = semesters.findByProgrammeIdOrderByNumberAsc(bca.getId()).stream()
                .filter(option -> option.getNumber() == 3).findFirst().orElseThrow();
        var rSubject = subjects.findByCode("BCA-STAT-301").orElseThrow();
        if (!resources.existsByDemoResourceTrueAndTitleIgnoreCase("Statistics with R example — summary values")) {
            resourceService.createDemoScript(bca, thirdSemester, rSubject,
                    "Statistics with R example — summary values",
                    "# Fictional sample resource\nscores <- c(62, 74, 81, 69, 90)\nmean(scores)\nmedian(scores)\nsummary(scores)\n");
        }
        if (!resources.existsByDemoResourceTrueAndTitleIgnoreCase("Statistics with R example — bar chart")) {
            resourceService.createDemoScript(bca, thirdSemester, rSubject,
                    "Statistics with R example — bar chart",
                    "# Fictional sample resource\nsubjects <- c(\"Java\", \"DBMS\", \"R\")\ncounts <- c(12, 9, 7)\nbarplot(counts, names.arg = subjects, col = \"seagreen\")\n");
        }
    }

    private record Sample(String programmeCode, int semester, String subjectCode, ResourceType type,
                          ExamType examType, String session, String title, String text) { }
}
