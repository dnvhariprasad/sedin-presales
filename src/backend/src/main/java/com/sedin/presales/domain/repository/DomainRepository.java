package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DomainRepository extends JpaRepository<Domain, UUID> {

    List<Domain> findByIsActiveTrue();

    List<Domain> findByNameContainingIgnoreCase(String name);
}
