package com.chat.modules.user.repository;

import com.chat.modules.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByAvatarFileId(UUID avatarFileId);

    long countByIdIn(Collection<UUID> ids);

    @Query(UserQueries.SEARCH_PUBLIC_PROFILES)
    Page<UserEntity> searchPublicProfiles(
            @Param("currentUserId") UUID currentUserId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(UserQueries.SEARCH_AVAILABLE_GROUP_MEMBERS)
    Page<UserEntity> searchAvailableGroupMembers(
            @Param("currentUserId") UUID currentUserId,
            @Param("conversationId") UUID conversationId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
