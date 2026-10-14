package edu.collegehub.catalog;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogueService {
    private final ProgrammeRepository programmes;
    private final SemesterRepository semesters;
    private final SubjectRepository subjects;
    public CatalogueService(ProgrammeRepository programmes, SemesterRepository semesters, SubjectRepository subjects) {
        this.programmes = programmes; this.semesters = semesters; this.subjects = subjects;
    }
    public List<Programme> programmes() { return programmes.findByActiveTrueOrderByNameAsc(); }
    public List<Programme> programmesWithSemesters() {
        return programmes().stream().filter(programme -> !semesters(programme.getId()).isEmpty()).toList();
    }
    public List<Semester> semesters(Long programmeId) {
        return programmeId == null ? List.of() : semesters.findByProgrammeIdOrderByNumberAsc(programmeId);
    }
    public List<SemesterOption> semesterOptions(Long programmeId) {
        return semesters(programmeId).stream().map(s -> new SemesterOption(s.getId(), s.getNumber(), s.getLabel())).toList();
    }
    public List<Subject> subjects(Long programmeId, Long semesterId) {
        if (programmeId == null) return List.of();
        return semesterId == null ? subjects.findByProgrammeIdAndActiveTrueOrderByNameAsc(programmeId)
                : subjects.findByProgrammeIdAndSemesterIdAndActiveTrueOrderByNameAsc(programmeId, semesterId);
    }
    public List<SubjectOption> subjectOptions(Long programmeId, Long semesterId) {
        return subjects(programmeId, semesterId).stream()
                .map(s -> new SubjectOption(s.getId(), s.getName(), s.getCode(), s.getSemester().getId())).toList();
    }
    public List<SubjectOption> subjectOptionsForSemester(Long semesterId) {
        Semester semester = semesters.findById(semesterId).orElse(null);
        return semester == null ? List.of() : subjectOptions(semester.getProgramme().getId(), semesterId);
    }
    public Semester semester(Long id) { return semesters.findById(id).orElse(null); }
    public Programme activeProgramme(Long id) {
        return id == null ? null : programmes.findById(id).filter(Programme::isActive).orElse(null);
    }
    public Semester semesterInProgramme(Long semesterId, Long programmeId) {
        return semesterId == null || programmeId == null ? null
                : semesters.findByIdAndProgrammeId(semesterId, programmeId).orElse(null);
    }
    public boolean validSelection(Long programmeId, Long semesterId, Long subjectId) {
        if (programmeId == null) {
            if (semesterId != null) return false;
            return subjectId == null || subjects.existsById(subjectId);
        }
        if (!programmes.existsById(programmeId)) return false;
        if (semesterId != null && !semesters.existsByIdAndProgrammeId(semesterId, programmeId)) return false;
        if (subjectId != null) {
            if (semesterId == null || !subjects.existsByIdAndProgrammeIdAndSemesterId(subjectId, programmeId, semesterId)) return false;
        }
        return true;
    }
    public Programme requireProgramme(Long id) {
        return programmes.findById(id).filter(Programme::isActive).orElseThrow(() -> new IllegalArgumentException("Choose a valid programme."));
    }
    public Semester requireSemester(Long programmeId, Long semesterId) {
        return semesters.findById(semesterId)
                .filter(s -> s.getProgramme().getId().equals(programmeId))
                .orElseThrow(() -> new IllegalArgumentException("Choose a semester belonging to the selected programme."));
    }
    public Subject requireSubject(Long programmeId, Long semesterId, Long subjectId) {
        return subjects.findById(subjectId)
                .filter(s -> s.getProgramme().getId().equals(programmeId) && s.getSemester().getId().equals(semesterId) && s.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Choose a subject belonging to the selected programme and semester."));
    }
    @Transactional
    public Subject resolveSubjectName(Programme programme, Semester semester, String rawName) {
        String name = rawName.trim().replaceAll("\\s+", " ");
        String normalized = Subject.normalize(name);
        return subjects.findByProgrammeIdAndSemesterIdAndNormalizedName(programme.getId(), semester.getId(), normalized)
                .orElseGet(() -> subjects.save(new Subject(programme, semester,
                        "USR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT), name)));
    }
    public List<SubjectSuggestion> subjectSuggestions(String term, Long programmeId, Long semesterId) {
        if (term == null || term.trim().length() < 2) return List.of();
        if (programmeId != null && programmes.findById(programmeId).filter(Programme::isActive).isEmpty()) return List.of();
        if (semesterId != null && (programmeId == null || !semesters.existsByIdAndProgrammeId(semesterId, programmeId))) return List.of();
        return subjects.findSuggestions(term.trim(), programmeId, semesterId,
                        PageRequest.of(0, 15, Sort.by(Sort.Order.asc("name"))))
                .stream().map(subject -> new SubjectSuggestion(subject.getId(), subject.getName(), subject.getCode(),
                        subject.getProgramme().getName(), subject.getSemester().getLabel()))
                .toList();
    }
    public Subject subject(Long id) { return id == null ? null : subjects.findById(id).orElse(null); }
    public record SemesterOption(Long id, int number, String label) { }
    public record SubjectOption(Long id, String name, String code, Long semesterId) { }
    public record SubjectSuggestion(Long id, String name, String code, String programme, String semester) { }
}
