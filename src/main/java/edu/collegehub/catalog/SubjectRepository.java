package edu.collegehub.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByProgrammeIdAndSemesterIdAndActiveTrueOrderByNameAsc(Long programmeId, Long semesterId);
    List<Subject> findByProgrammeIdAndActiveTrueOrderByNameAsc(Long programmeId);
    boolean existsByIdAndProgrammeIdAndSemesterId(Long id, Long programmeId, Long semesterId);
    java.util.Optional<Subject> findByCode(String code);
    Optional<Subject> findByProgrammeIdAndSemesterIdAndNormalizedName(Long programmeId, Long semesterId, String normalizedName);

    @Query("select distinct s from Subject s left join Resource r on r.subject = s " +
            "where (lower(s.name) like lower(concat('%', :term, '%')) or lower(s.code) like lower(concat('%', :term, '%'))) " +
            "and (s.active = true or r.id is not null) " +
            "and (:programmeId is null or s.programme.id = :programmeId) " +
            "and (:semesterId is null or s.semester.id = :semesterId)")
    List<Subject> findSuggestions(@Param("term") String term, @Param("programmeId") Long programmeId,
                                  @Param("semesterId") Long semesterId, Pageable pageable);
}
