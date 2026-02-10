package com.umc.devine.domain.project.repository.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.entity.QProject;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.techstack.entity.mapping.QProjectRequirementTechstack;

public class ProjectPredicateBuilder {

    // 프로젝트 검색 조건 생성
    public static Predicate buildSearchPredicate(ProjectReqDTO.SearchProjectReq req) {
        QProject project = QProject.project;
        QProjectRequirementTechstack techstack = QProjectRequirementTechstack.projectRequirementTechstack;

        BooleanBuilder builder = new BooleanBuilder();

        // 삭제되지 않은 프로젝트만
        builder.and(project.status.ne(ProjectStatus.DELETED));

        // 프로젝트 분야 필터 (비어있거나 ALL 포함 시 전체 조회)
        if (req.projectFields() != null && !req.projectFields().isEmpty()
                && !req.projectFields().contains(ProjectField.ALL)) {
            builder.and(project.projectField.in(req.projectFields()));
        }

        // 카테고리 필터 (비어있거나 ALL 포함 시 전체 조회)
        if (req.categories() != null && !req.categories().isEmpty()
                && !req.categories().contains(CategoryGenre.ALL)) {
            builder.and(project.category.genre.in(req.categories()));
        }

        // 진행 기간 필터 (복수 선택 가능)
        if (req.durationRanges() != null && !req.durationRanges().isEmpty()) {
            builder.and(project.durationRange.in(req.durationRanges()));
        }

        // 포지션 필터 (비어있거나 ALL 포함 시 전체 조회)
        if (req.positions() != null && !req.positions().isEmpty()
                && !req.positions().contains(ProjectPart.ALL)) {
            builder.and(project.requirements.any().part.in(req.positions()));
        }

        // 기술 스택 필터 (복수 선택 가능)
        if (req.techstackNames() != null && !req.techstackNames().isEmpty()) {
            builder.and(project.id.in(
                    com.querydsl.jpa.JPAExpressions
                            .select(techstack.requirement.project.id)
                            .from(techstack)
                            .where(techstack.techstack.name.in(req.techstackNames()))
            ));
        }

        return builder;
    }

    // 추천 프로젝트 필터 조건 생성
    public static Predicate buildRecommendPredicate(ProjectReqDTO.RecommendProjectsReq req) {
        QProject project = QProject.project;
        QProjectRequirementTechstack techstack = QProjectRequirementTechstack.projectRequirementTechstack;

        BooleanBuilder builder = new BooleanBuilder();

        // 모집 중인 프로젝트만
        builder.and(project.status.eq(ProjectStatus.RECRUITING));

        // 프로젝트 분야 필터 (비어있거나 ALL 포함 시 전체 조회)
        if (req.projectFields() != null && !req.projectFields().isEmpty()
                && !req.projectFields().contains(ProjectField.ALL)) {
            builder.and(project.projectField.in(req.projectFields()));
        }

        // 카테고리 필터 (비어있거나 ALL 포함 시 전체 조회)
        if (req.categories() != null && !req.categories().isEmpty()
                && !req.categories().contains(CategoryGenre.ALL)) {
            builder.and(project.category.genre.in(req.categories()));
        }

        // 진행 기간 필터 (복수 선택 가능)
        if (req.durationRanges() != null && !req.durationRanges().isEmpty()) {
            builder.and(project.durationRange.in(req.durationRanges()));
        }

        // 기술 스택 필터 (복수 선택 가능)
        if (req.techstackNames() != null && !req.techstackNames().isEmpty()) {
            builder.and(project.id.in(
                    com.querydsl.jpa.JPAExpressions
                            .select(techstack.requirement.project.id)
                            .from(techstack)
                            .where(techstack.techstack.name.in(req.techstackNames()))
            ));
        }

        return builder;
    }
}