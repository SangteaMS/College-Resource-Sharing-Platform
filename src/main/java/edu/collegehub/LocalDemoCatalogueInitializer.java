package edu.collegehub;

import edu.collegehub.catalog.Programme;
import edu.collegehub.catalog.ProgrammeRepository;
import edu.collegehub.catalog.Semester;
import edu.collegehub.catalog.SemesterRepository;
import edu.collegehub.catalog.Subject;
import edu.collegehub.catalog.SubjectRepository;
import edu.collegehub.resources.FileFormat;
import edu.collegehub.resources.ModerationStatus;
import edu.collegehub.resources.Resource;
import edu.collegehub.resources.ResourceRepository;
import edu.collegehub.resources.ResourceType;
import edu.collegehub.resources.UploadStorageService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Adds browseable illustrative catalogue entries only for local development. */
@Component
@Profile("local")
public class LocalDemoCatalogueInitializer implements ApplicationRunner {
    private final ProgrammeRepository programmes;
    private final SemesterRepository semesters;
    private final SubjectRepository subjects;
    private final ResourceRepository resources;
    private final UploadStorageService storage;

    public LocalDemoCatalogueInitializer(ProgrammeRepository programmes, SemesterRepository semesters,
                                         SubjectRepository subjects, ResourceRepository resources,
                                         UploadStorageService storage) {
        this.programmes = programmes;
        this.semesters = semesters;
        this.subjects = subjects;
        this.resources = resources;
        this.storage = storage;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<CourseSpec> courses = courses();
        for (CourseSpec spec : courses) {
            Programme programme = programmes.findByCode(spec.code()).orElseGet(() ->
                    programmes.save(new Programme(spec.code(), spec.name(), "Illustrative demo programme")));
            for (int number = 1; number <= 6; number++) {
                int semesterNumber = number;
                if (semesters.findByProgrammeIdOrderByNumberAsc(programme.getId()).stream()
                        .noneMatch(semester -> semester.getNumber() == semesterNumber)) {
                    semesters.save(new Semester(programme, number));
                }
            }
        }

        for (SubjectSpec spec : subjects()) {
            Programme programme = requireProgramme(spec.programmeCode());
            Semester semester = semesters.findByProgrammeIdOrderByNumberAsc(programme.getId()).stream()
                    .filter(candidate -> candidate.getNumber() == spec.semesterNumber()).findFirst().orElseThrow();
            Subject subject = subjects.findByCode(spec.code()).orElseGet(() ->
                    subjects.save(new Subject(programme, semester, spec.code(), spec.name())));
            if (!subject.getProgramme().getId().equals(programme.getId())
                    || !subject.getSemester().getId().equals(semester.getId())) {
                throw new IllegalStateException("A demo subject code already exists in a different course or semester: " + spec.code());
            }
        }

        byte[] samplePdf = samplePdf();
        for (ResourceSpec spec : resources()) {
            if (resources.existsByTitle(spec.title())) continue;
            Programme programme = requireProgramme(spec.programmeCode());
            Semester semester = semesters.findByProgrammeIdOrderByNumberAsc(programme.getId()).stream()
                    .filter(candidate -> candidate.getNumber() == spec.semesterNumber()).findFirst().orElseThrow();
            Subject subject = subjects.findByCode(spec.subjectCode()).orElseThrow();
            String filename = spec.title().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") + ".pdf";
            String oldTitle = spec.title().replaceFirst("^Sample: ", "Demo: ");
            Resource resource = resources.findByTitleAndUploadedByIsNull(oldTitle).orElse(null);
            if (resource != null) {
                String oldStoredFilename = resource.getStoredFilename();
                if (resource.isRemoved()) {
                    resource.updateSampleContent(spec.title(), "Fictional sample material. Not an official college document.",
                            FileFormat.PDF, resource.getDisplayFilename(), oldStoredFilename, resource.getFileSizeBytes());
                } else {
                    var stored = storage.store(new UploadStorageService.ValidatedUpload(filename, FileFormat.PDF, samplePdf));
                    resource.updateSampleContent(spec.title(), "Fictional sample material. Not an official college document.",
                            stored.format(), stored.displayFilename(), stored.storedFilename(), stored.size());
                    if (oldStoredFilename != null) storage.deleteAfterCommit(storage.pathFor(oldStoredFilename));
                }
                resources.save(resource);
                continue;
            }
            var stored = storage.store(new UploadStorageService.ValidatedUpload(filename, FileFormat.PDF, samplePdf));
            resource = new Resource();
            resource.setDetails(spec.title(), "Fictional sample material. Not an official college document.",
                    programme, semester, subject, spec.type(), stored.format(), stored.displayFilename(),
                    stored.storedFilename(), stored.size(), null, null, null);
            resource.setDemoResource(false);
            resource.approve(null);
            resources.save(resource);
        }
    }

    private Programme requireProgramme(String code) {
        return programmes.findByCode(code).orElseThrow(() -> new IllegalStateException("Missing demo course " + code));
    }

    private List<CourseSpec> courses() {
        return List.of(
                new CourseSpec("BSCCS", "B.Sc. Computer Science"), new CourseSpec("BSCMATH", "B.Sc. Mathematics"),
                new CourseSpec("BSCPHY", "B.Sc. Physics"), new CourseSpec("BSCCHEM", "B.Sc. Chemistry"),
                new CourseSpec("BSCDS", "B.Sc. Data Science"), new CourseSpec("BSCZOO", "B.Sc. Zoology"),
                new CourseSpec("BSCBOT", "B.Sc. Botany"), new CourseSpec("BAENG", "B.A. English"),
                new CourseSpec("BAPSY", "B.A. Psychology"), new CourseSpec("BAECO", "B.A. Economics"),
                new CourseSpec("BAHIST", "B.A. History"), new CourseSpec("BASOC", "B.A. Sociology"));
    }

    private List<SubjectSpec> subjects() {
        return List.of(
                new SubjectSpec("BCA", 1, "BCA-JAVA-101", "Java Programming"),
                new SubjectSpec("BCA", 2, "BCA-DBMS-201", "Database Management Systems"),
                new SubjectSpec("BCA", 3, "BCA-STAT-301", "Statistics with R"),
                new SubjectSpec("BCOM", 1, "BCOM-ACC-101", "Financial Accounting"),
                new SubjectSpec("BCOM", 2, "BCOM-ECO-201", "Business Economics"),
                new SubjectSpec("BBA", 1, "BBA-MGMT-101", "Principles of Management"),
                new SubjectSpec("BBA", 2, "BBA-MKT-201", "Marketing Management"),
                new SubjectSpec("BAPOL", 1, "BAPOL-POL-101", "Political Theory"),
                new SubjectSpec("BAPOL", 2, "BAPOL-GOV-201", "Indian Government and Politics"),
                new SubjectSpec("BSCBIO", 1, "BSCBIO-CELL-101", "Cell Biology"),
                new SubjectSpec("BSCBIO", 2, "BSCBIO-GEN-201", "Genetics"),
                new SubjectSpec("BSCCS", 1, "BSCCS-PROG-101", "Programming Fundamentals"),
                new SubjectSpec("BSCCS", 2, "BSCCS-DSA-201", "Data Structures and Algorithms"),
                new SubjectSpec("BSCMATH", 1, "BSCMATH-CALC-101", "Calculus I"),
                new SubjectSpec("BSCMATH", 2, "BSCMATH-LA-201", "Linear Algebra"),
                new SubjectSpec("BSCPHY", 1, "BSCPHY-MECH-101", "Classical Mechanics"),
                new SubjectSpec("BSCPHY", 2, "BSCPHY-OPT-201", "Waves and Optics"),
                new SubjectSpec("BSCCHEM", 1, "BSCCHEM-GEN-101", "General Chemistry"),
                new SubjectSpec("BSCCHEM", 2, "BSCCHEM-ORG-201", "Organic Chemistry"),
                new SubjectSpec("BSCDS", 1, "BSCDS-STAT-101", "Statistics for Data Science"),
                new SubjectSpec("BSCDS", 2, "BSCDS-PY-201", "Python for Data Analysis"),
                new SubjectSpec("BSCZOO", 1, "BSCZOO-DIV-101", "Animal Diversity"),
                new SubjectSpec("BSCZOO", 2, "BSCZOO-PHY-201", "Animal Physiology"),
                new SubjectSpec("BSCBOT", 1, "BSCBOT-DIV-101", "Plant Diversity"),
                new SubjectSpec("BSCBOT", 2, "BSCBOT-PHY-201", "Plant Physiology"),
                new SubjectSpec("BAENG", 1, "BAENG-WRIT-101", "Academic Writing"),
                new SubjectSpec("BAENG", 2, "BAENG-LIT-201", "Introduction to Literature"),
                new SubjectSpec("BAPSY", 1, "BAPSY-INTRO-101", "Introduction to Psychology"),
                new SubjectSpec("BAPSY", 2, "BAPSY-DEV-201", "Developmental Psychology"),
                new SubjectSpec("BAECO", 1, "BAECO-MICRO-101", "Microeconomics"),
                new SubjectSpec("BAECO", 2, "BAECO-MACRO-201", "Macroeconomics"),
                new SubjectSpec("BAHIST", 1, "BAHIST-ANC-101", "Ancient Civilizations"),
                new SubjectSpec("BAHIST", 2, "BAHIST-MOD-201", "Modern Indian History"),
                new SubjectSpec("BASOC", 1, "BASOC-INTRO-101", "Introduction to Sociology"),
                new SubjectSpec("BASOC", 2, "BASOC-RES-201", "Social Research Methods"));
    }

    private List<ResourceSpec> resources() {
        List<ResourceSpec> result = new ArrayList<>();
        add(result, "BCA", 1, "BCA-JAVA-101", "Sample: Java Programming Quick Notes", ResourceType.NOTES);
        add(result, "BCA", 1, "BCA-JAVA-101", "Sample: Java Objects and Classes Slides", ResourceType.PRESENTATION);
        add(result, "BCA", 2, "BCA-DBMS-201", "Sample: Database Normalization Practice Paper", ResourceType.PRACTICE_MATERIAL);
        add(result, "BCA", 3, "BCA-STAT-301", "Sample: Statistics with R Study Guide", ResourceType.STUDY_GUIDE);
        add(result, "BCOM", 1, "BCOM-ACC-101", "Sample: Accounting Ledger Notes", ResourceType.NOTES);
        add(result, "BCOM", 2, "BCOM-ECO-201", "Sample: Business Economics Assignment", ResourceType.ASSIGNMENT);
        add(result, "BBA", 1, "BBA-MGMT-101", "Sample: Management Principles Study Material", ResourceType.STUDY_MATERIAL);
        add(result, "BBA", 2, "BBA-MKT-201", "Sample: Marketing Mix Slides", ResourceType.PRESENTATION);
        add(result, "BAPOL", 1, "BAPOL-POL-101", "Sample: Political Theory Reading Notes", ResourceType.NOTES);
        add(result, "BAPOL", 2, "BAPOL-GOV-201", "Sample: Indian Government Revision Guide", ResourceType.STUDY_GUIDE);
        add(result, "BSCBIO", 1, "BSCBIO-CELL-101", "Sample: Cell Biology Diagram Slides", ResourceType.PRESENTATION);
        add(result, "BSCBIO", 2, "BSCBIO-GEN-201", "Sample: Genetics Practice Questions", ResourceType.PRACTICE_MATERIAL);
        add(result, "BSCCS", 1, "BSCCS-PROG-101", "Sample: Programming Fundamentals Notes", ResourceType.NOTES);
        add(result, "BSCCS", 2, "BSCCS-DSA-201", "Sample: Data Structures Assignment", ResourceType.ASSIGNMENT);
        add(result, "BSCMATH", 1, "BSCMATH-CALC-101", "Sample: Calculus Worked Examples", ResourceType.STUDY_MATERIAL);
        add(result, "BSCMATH", 2, "BSCMATH-LA-201", "Sample: Linear Algebra Practice Paper", ResourceType.PRACTICE_MATERIAL);
        add(result, "BSCPHY", 1, "BSCPHY-MECH-101", "Sample: Classical Mechanics Study Guide", ResourceType.STUDY_GUIDE);
        add(result, "BSCPHY", 2, "BSCPHY-OPT-201", "Sample: Waves and Optics Slides", ResourceType.PRESENTATION);
        add(result, "BSCCHEM", 1, "BSCCHEM-GEN-101", "Sample: General Chemistry Notes", ResourceType.NOTES);
        add(result, "BSCCHEM", 2, "BSCCHEM-ORG-201", "Sample: Organic Chemistry Lab Worksheet", ResourceType.LAB_PRACTICAL_MATERIAL);
        add(result, "BSCDS", 1, "BSCDS-STAT-101", "Sample: Data Science Statistics Assignment", ResourceType.ASSIGNMENT);
        add(result, "BSCDS", 2, "BSCDS-PY-201", "Sample: Python Data Analysis Practice", ResourceType.PRACTICE_MATERIAL);
        add(result, "BSCZOO", 1, "BSCZOO-DIV-101", "Sample: Animal Diversity Study Material", ResourceType.STUDY_MATERIAL);
        add(result, "BSCZOO", 2, "BSCZOO-PHY-201", "Sample: Animal Physiology Revision Notes", ResourceType.NOTES);
        add(result, "BSCBOT", 1, "BSCBOT-DIV-101", "Sample: Plant Diversity Slides", ResourceType.PRESENTATION);
        add(result, "BSCBOT", 2, "BSCBOT-PHY-201", "Sample: Plant Physiology Practice Questions", ResourceType.PRACTICE_MATERIAL);
        add(result, "BAENG", 1, "BAENG-WRIT-101", "Sample: Academic Writing Study Guide", ResourceType.STUDY_GUIDE);
        add(result, "BAENG", 2, "BAENG-LIT-201", "Sample: Introduction to Literature Notes", ResourceType.NOTES);
        add(result, "BAPSY", 1, "BAPSY-INTRO-101", "Sample: Psychology Concepts Slides", ResourceType.PRESENTATION);
        add(result, "BAECO", 1, "BAECO-MICRO-101", "Sample: Microeconomics Practice Paper", ResourceType.PRACTICE_MATERIAL);
        add(result, "BAHIST", 2, "BAHIST-MOD-201", "Sample: Modern Indian History Assignment", ResourceType.ASSIGNMENT);
        add(result, "BASOC", 2, "BASOC-RES-201", "Sample: Social Research Methods Notes", ResourceType.NOTES);
        return result;
    }

    private void add(List<ResourceSpec> target, String programme, int semester, String subject, String title, ResourceType type) {
        target.add(new ResourceSpec(programme, semester, subject, title, type));
    }

    private byte[] samplePdf() {
        String content = "BT /F1 16 Tf 72 720 Td (Fictional sample material. Not an official college document.) Tj ET\n";
        String stream = "<< /Length " + content.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + content + "endstream";
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                stream);
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        write(pdf, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.size());
            write(pdf, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }
        int xref = pdf.size();
        write(pdf, "xref\n0 6\n0000000000 65535 f \n");
        offsets.forEach(offset -> write(pdf, String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offset)));
        write(pdf, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return pdf.toByteArray();
    }

    private void write(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private record CourseSpec(String code, String name) { }
    private record SubjectSpec(String programmeCode, int semesterNumber, String code, String name) { }
    private record ResourceSpec(String programmeCode, int semesterNumber, String subjectCode, String title, ResourceType type) { }
}
