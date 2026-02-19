package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, UUID> {

    List<Industry> findByIsActiveTrue();

    List<Industry> findByNameContainingIgnoreCase(String name);
}
