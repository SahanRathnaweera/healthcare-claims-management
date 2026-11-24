package com.healthcare.claimsmanagement.repository;

import com.healthcare.claimsmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRoleRoleNameAndActiveTrue(String roleName);

    @Query("select u from User u where u.role.roleName = :roleName and u.active = true")
    java.util.List<User> findActiveByRoleName(@Param("roleName") String roleName);
}
