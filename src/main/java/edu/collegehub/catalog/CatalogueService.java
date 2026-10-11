package edu.collegehub.catalog;

import java.util.List;

import org.springframework.stereotype.Service;
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
    public boolean validSelection(Long programmeId, Long semesterId, Long subjectId) {
        if (programmeId == null) return semesterId == null && subjectId == null;
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
    public record SemesterOption(Long id, int number, String label) { }
    public record SubjectOption(Long id, String name, String code, Long semesterId) { }
}
