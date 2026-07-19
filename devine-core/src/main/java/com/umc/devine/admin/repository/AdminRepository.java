package com.umc.devine.admin.repository;

import com.umc.devine.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    @Query("SELECT a FROM Admin a WHERE a.clerkId = :clerkId AND a.isActive = true")
    Optional<Admin> findActiveByClerkId(@Param("clerkId") String clerkId);

    @Query("SELECT a FROM Admin a WHERE a.email = :email AND a.isActive = true")
    Optional<Admin> findActiveByEmail(@Param("email") String email);
}