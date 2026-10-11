package edu.collegehub.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByProgrammeIdAndSemesterIdAndActiveTrueOrderByNameAsc(Long programmeId, Long semesterId);
    List<Subject> findByProgrammeIdAndActiveTrueOrderByNameAsc(Long programmeId);
    boolean existsByIdAndProgrammeIdAndSemesterId(Long id, Long programmeId, Long semesterId);
    java.util.Optional<Subject> findByCode(String code);
}
