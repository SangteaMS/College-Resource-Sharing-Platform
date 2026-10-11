package edu.collegehub.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgrammeRepository extends JpaRepository<Programme, Long> {
    List<Programme> findByActiveTrueOrderByNameAsc();
    java.util.Optional<Programme> findByCode(String code);
}
