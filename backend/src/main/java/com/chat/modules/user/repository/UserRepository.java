package com.chat.modules.user.repository;

import com.chat.modules.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByAvatarFileId(UUID avatarFileId);

    @Query(UserQueries.SEARCH_PUBLIC_PROFILES)
    Page<UserEntity> searchPublicProfiles(
            @Param("currentUserId") UUID currentUserId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
