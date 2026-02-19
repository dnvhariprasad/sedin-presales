package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.Sbu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SbuRepository extends JpaRepository<Sbu, UUID> {

    List<Sbu> findByIsActiveTrue();

    List<Sbu> findByNameContainingIgnoreCase(String name);
}
