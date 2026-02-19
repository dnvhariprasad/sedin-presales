package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.Technology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TechnologyRepository extends JpaRepository<Technology, UUID> {

    List<Technology> findByIsActiveTrue();

    List<Technology> findByNameContainingIgnoreCase(String name);
}
