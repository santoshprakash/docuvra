package com.docuvra.repository;

import com.docuvra.entity.UserEntity;
import com.docuvra.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    Optional<UserEntity> findByEmailIgnoreCaseAndRole(String email, UserRole role);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCaseAndRole(String email, UserRole role);

    boolean existsByMobileAndRole(String mobile, UserRole role);

    List<UserEntity> findAllByOrderByCreatedAtDesc();

    List<UserEntity> findAllByRoleAndActiveTrue(UserRole role);
}
