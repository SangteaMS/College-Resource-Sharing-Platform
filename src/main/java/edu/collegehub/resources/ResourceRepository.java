package edu.collegehub.resources;

import java.util.List;
import java.util.Optional;

import edu.collegehub.accounts.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    @Query("select distinct r.examYear from Resource r where r.status = edu.collegehub.resources.ModerationStatus.APPROVED and r.removed = false and r.resourceType = edu.collegehub.resources.ResourceType.QUESTION_PAPER and r.examYear is not null order by r.examYear desc")
    List<Integer> findApprovedQuestionPaperYears();

    @EntityGraph(attributePaths = {"programme", "semester", "subject"})
    @Query("""
            select r from Resource r join r.subject s
            where r.status = edu.collegehub.resources.ModerationStatus.APPROVED and r.removed = false
              and (:questionPapersOnly = false or r.resourceType = edu.collegehub.resources.ResourceType.QUESTION_PAPER)
              and (:programmeId is null or r.programme.id = :programmeId)
              and (:semesterId is null or r.semester.id = :semesterId)
              and (:subjectId is null or s.id = :subjectId)
              and (:resourceType is null or r.resourceType = :resourceType)
              and (:examYear is null or r.examYear = :examYear)
              and (:examType is null or r.examType = :examType)
              and (:queryText is null or lower(r.title) like lower(concat('%', :queryText, '%'))
                   or lower(coalesce(r.description, '')) like lower(concat('%', :queryText, '%'))
                   or lower(s.name) like lower(concat('%', :queryText, '%'))
                   or lower(s.code) like lower(concat('%', :queryText, '%')))
            """)
    Page<Resource> searchApproved(@Param("programmeId") Long programmeId,
                                  @Param("semesterId") Long semesterId,
                                  @Param("subjectId") Long subjectId,
                                  @Param("resourceType") ResourceType resourceType,
                                  @Param("examYear") Integer examYear,
                                  @Param("examType") ExamType examType,
                                  @Param("queryText") String queryText,
                                  @Param("questionPapersOnly") boolean questionPapersOnly,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"programme", "semester", "subject"})
    Optional<Resource> findByIdAndStatusAndRemovedFalse(Long id, ModerationStatus status);
    @EntityGraph(attributePaths = {"programme", "semester", "subject"})
    Optional<Resource> findByIdAndUploadedBy_IdAndRemovedFalse(Long id, Long userId);
    @EntityGraph(attributePaths = {"programme", "semester", "subject"})
    List<Resource> findByUploadedByAndRemovedFalseOrderByUploadedAtDesc(AppUser uploadedBy);
    @EntityGraph(attributePaths = {"programme", "semester", "subject", "uploadedBy"})
    List<Resource> findByStatusAndRemovedFalseOrderByUploadedAtAsc(ModerationStatus status);
    List<Resource> findByProgrammeIdAndSubjectIdAndResourceTypeAndDemoResourceTrue(Long programmeId, Long subjectId, ResourceType type);
    boolean existsByDemoResourceTrueAndRemovedFalse();
    boolean existsByDemoResourceTrueAndTitleIgnoreCase(String title);
}
