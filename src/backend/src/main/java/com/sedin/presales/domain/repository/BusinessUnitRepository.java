package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, UUID> {

    List<BusinessUnit> findByIsActiveTrue();

    List<BusinessUnit> findByNameContainingIgnoreCase(String name);
}
