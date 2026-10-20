package edu.collegehub.resources;

import java.util.List;
import java.util.Optional;

import edu.collegehub.accounts.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {
    @Query("select distinct r.examYear from Resource r where r.status = edu.collegehub.resources.ModerationStatus.APPROVED and r.removed = false and r.demoResource = false and r.resourceType = edu.collegehub.resources.ResourceType.QUESTION_PAPER and r.examYear is not null order by r.examYear desc")
    List<Integer> findApprovedQuestionPaperYears();

    @Override
    @EntityGraph(attributePaths = {"programme", "semester", "subject"})
    Page<Resource> findAll(Specification<Resource> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"programme", "semester", "subject"})
    Optional<Resource> findByIdAndStatusAndRemovedFalseAndDemoResourceFalse(Long id, ModerationStatus status);
    boolean existsByTitle(String title);
    Optional<Resource> findByTitleAndUploadedByIsNull(String title);
    @EntityGraph(attributePaths = {"programme", "semester", "subject"})
    Optional<Resource> findByIdAndUploadedBy_IdAndRemovedFalse(Long id, Long userId);
    @EntityGraph(attributePaths = {"programme", "semester", "subject"})
    List<Resource> findByUploadedByAndRemovedFalseOrderByUploadedAtDesc(AppUser uploadedBy);
    @EntityGraph(attributePaths = {"programme", "semester", "subject", "removedBy"})
    List<Resource> findByUploadedByOrderByUploadedAtDesc(AppUser uploadedBy);
    @EntityGraph(attributePaths = {"programme", "semester", "subject", "uploadedBy"})
    List<Resource> findByStatusAndRemovedFalseOrderByUploadedAtAsc(ModerationStatus status);
}
