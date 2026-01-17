package com.umc.devine.domain.project.repository.querydsl;

import com.querydsl.core.types.Predicate;
import com.umc.devine.domain.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectQueryDsl {

    // 프로젝트 검색 (필터링)
    Page<Project> searchProjects(Predicate predicate, Pageable pageable);

    // 추천 프로젝트 검색 (필터링)
    Page<Project> searchRecommendedProjects(Predicate predicate, Pageable pageable);

    // 삭제되지 않은 프로젝트 전체 조회 (추천용)
    List<Project> findAllActiveProjects(int limit);
}