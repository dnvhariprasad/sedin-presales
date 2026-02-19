package com.sedin.presales.domain.repository;

import com.sedin.presales.domain.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByParentId(UUID parentId);

    List<Folder> findByParentIsNull();
}
