package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.techstack.entity.Techstack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.devine.domain.techstack.enums.TechName;

import java.util.List;
import java.util.Optional;

public interface TechstackRepository extends JpaRepository<Techstack, Long> {

    Optional<Techstack> findByName(TechName name);

    List<Techstack> findAllByNameIn(List<TechName> names);

    @Query("SELECT t FROM Techstack t LEFT JOIN FETCH t.parentStack WHERE t.name IN :names")
    List<Techstack> findAllByNameInWithParent(@Param("names") List<TechName> names);

    Optional<Techstack> findByNameAndParentStackName(TechName name, TechName parentName);

    List<Techstack> findAllByParentStackIsNull();

    List<Techstack> findAllByParentStack(Techstack parentStack);

    @Query("SELECT t FROM Techstack t LEFT JOIN FETCH t.parentStack")
    List<Techstack> findAllWithParent();
}