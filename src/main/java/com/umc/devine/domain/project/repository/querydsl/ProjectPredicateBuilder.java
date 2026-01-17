package com.umc.devine.domain.project.repository.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.entity.QProject;
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

        // 프로젝트 분야 필터
        if (req.projectFields() != null && !req.projectFields().isEmpty()) {
            builder.and(project.projectField.in(req.projectFields()));
        }

        // 카테고리 필터
        if (req.categoryIds() != null && !req.categoryIds().isEmpty()) {
            builder.and(project.category.id.in(req.categoryIds()));
        }

        // 진행 기간 필터 (범위로 선택)
        if (req.durationRange() != null) {
            builder.and(project.durationMonths.goe(req.durationRange().getMinMonths()));
            builder.and(project.durationMonths.loe(req.durationRange().getMaxMonths()));
        }

        // 포지션 필터
        if (req.positions() != null && !req.positions().isEmpty()) {
            builder.and(project.requirements.any().part.in(req.positions()));
        }

        // 기술 스택 필터
        if (req.techStackIds() != null && !req.techStackIds().isEmpty()) {
            builder.and(project.id.in(
                    com.querydsl.jpa.JPAExpressions
                            .select(techstack.requirement.project.id)
                            .from(techstack)
                            .where(techstack.techstack.id.in(req.techStackIds()))
            ));
        }

        return builder;
    }

    // 추천 프로젝트 검색 조건 생성
    public static Predicate buildRecommendedPredicate(ProjectReqDTO.SearchRecommendedProjectReq req) {
        QProject project = QProject.project;
        QProjectRequirementTechstack techstack = QProjectRequirementTechstack.projectRequirementTechstack;

        BooleanBuilder builder = new BooleanBuilder();

        // 삭제되지 않은 프로젝트만
        builder.and(project.status.ne(ProjectStatus.DELETED));

        // 프로젝트 분야 필터
        if (req.projectFields() != null && !req.projectFields().isEmpty()) {
            builder.and(project.projectField.in(req.projectFields()));
        }

        // 카테고리 필터
        if (req.categoryIds() != null && !req.categoryIds().isEmpty()) {
            builder.and(project.category.id.in(req.categoryIds()));
        }

        // 진행 기간 필터 (범위로 선택)
        if (req.durationRange() != null) {
            builder.and(project.durationMonths.goe(req.durationRange().getMinMonths()));
            builder.and(project.durationMonths.loe(req.durationRange().getMaxMonths()));
        }

        // 포지션 필터
        if (req.positions() != null && !req.positions().isEmpty()) {
            builder.and(project.requirements.any().part.in(req.positions()));
        }

        // 기술 스택 필터
        if (req.techStackIds() != null && !req.techStackIds().isEmpty()) {
            builder.and(project.id.in(
                    com.querydsl.jpa.JPAExpressions
                            .select(techstack.requirement.project.id)
                            .from(techstack)
                            .where(techstack.techstack.id.in(req.techStackIds()))
            ));
        }

        return builder;
    }
}