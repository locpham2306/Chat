package com.chat.modules.file.repository;

import com.chat.modules.file.entity.FileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select file from FileEntity file where file.id in :ids")
    List<FileEntity> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);
}
