package edu.collegehub.usage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
    @EntityGraph(attributePaths = {"resource", "resource.programme", "resource.semester", "resource.subject"})
    List<UsageEvent> findAllByOrderByEventAtAsc();
}
