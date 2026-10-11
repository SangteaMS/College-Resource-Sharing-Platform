package edu.collegehub.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<Semester, Long> {
    List<Semester> findByProgrammeIdOrderByNumberAsc(Long programmeId);
    boolean existsByIdAndProgrammeId(Long id, Long programmeId);
}
