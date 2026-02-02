package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MatchingRepository extends JpaRepository<Matching, Long> {

    @Query("SELECT m FROM Matching m JOIN FETCH m.project JOIN FETCH m.member " +
            "WHERE m.project = :project AND m.member = :member AND m.matchingType = :matchingType AND m.status <> :excludeStatus")
    Optional<Matching> findByProjectAndMemberAndMatchingTypeAndStatusNot(
            @Param("project") Project project,
            @Param("member") Member member,
            @Param("matchingType") MatchingType matchingType,
            @Param("excludeStatus") MatchingStatus excludeStatus);

    @Query("SELECT COUNT(m) > 0 FROM Matching m " +
            "WHERE m.project = :project AND m.member = :member AND m.matchingType = :matchingType AND m.status <> :excludeStatus")
    boolean existsByProjectAndMemberAndMatchingTypeAndStatusNot(
            @Param("project") Project project,
            @Param("member") Member member,
            @Param("matchingType") MatchingType matchingType,
            @Param("excludeStatus") MatchingStatus excludeStatus);
}
