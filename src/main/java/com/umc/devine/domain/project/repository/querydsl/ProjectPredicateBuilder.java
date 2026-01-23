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

        // 진행 기간 필터 (수정된 범위: 1개월 이하, 1~3개월, 3~6개월, 6개월 이상)
        if (req.durationRange() != null) {
            int minMonths = req.durationRange().getMinMonths();
            int maxMonths = req.durationRange().getMaxMonths();

            // 6개월 이상인 경우
            if (maxMonths == Integer.MAX_VALUE) {
                builder.and(project.durationMonths.goe(minMonths));
            }
            // 1개월 이하인 경우
            else if (minMonths == 0) {
                builder.and(project.durationMonths.loe(maxMonths));
            }
            // 1~3개월, 3~6개월인 경우
            else {
                builder.and(project.durationMonths.goe(minMonths));
                builder.and(project.durationMonths.loe(maxMonths));
            }
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

    // 통합 추천 프로젝트 조회 조건 생성
    public static Predicate buildRecommendPredicate(ProjectReqDTO.RecommendProjectsReq req) {
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

        // 진행 기간 필터 (수정된 범위: 1개월 이하, 1~3개월, 3~6개월, 6개월 이상)
        if (req.durationRange() != null) {
            int minMonths = req.durationRange().getMinMonths();
            int maxMonths = req.durationRange().getMaxMonths();

            // 6개월 이상인 경우
            if (maxMonths == Integer.MAX_VALUE) {
                builder.and(project.durationMonths.goe(minMonths));
            }
            // 1개월 이하인 경우
            else if (minMonths == 0) {
                builder.and(project.durationMonths.loe(maxMonths));
            }
            // 1~3개월, 3~6개월인 경우
            else {
                builder.and(project.durationMonths.goe(minMonths));
                builder.and(project.durationMonths.loe(maxMonths));
            }
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