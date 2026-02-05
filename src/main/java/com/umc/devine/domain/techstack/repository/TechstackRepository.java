package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.techstack.entity.Techstack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.umc.devine.domain.techstack.enums.TechName;

import java.util.List;
import java.util.Optional;

public interface TechstackRepository extends JpaRepository<Techstack, Long> {

    Optional<Techstack> findByName(TechName name);

    List<Techstack> findAllByParentStackIsNull();

    List<Techstack> findAllByParentStack(Techstack parentStack);

    @Query("SELECT t FROM Techstack t LEFT JOIN FETCH t.parentStack")
    List<Techstack> findAllWithParent();
}