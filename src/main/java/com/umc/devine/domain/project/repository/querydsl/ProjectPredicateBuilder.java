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
        if (req.projectField() != null) {
            builder.and(project.projectField.eq(req.projectField()));
        }

        // 카테고리 필터
        if (req.category() != null) {
            builder.and(project.category.genre.eq(req.category()));
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
        if (req.position() != null) {
            builder.and(project.requirements.any().part.eq(req.position()));
        }

        // 기술 스택 필터
        if (req.techstackName() != null) {
            builder.and(project.id.in(
                    com.querydsl.jpa.JPAExpressions
                            .select(techstack.requirement.project.id)
                            .from(techstack)
                            .where(techstack.techstack.name.eq(req.techstackName()))
            ));
        }

        return builder;
    }

    // 추천 프로젝트 페이지 조회 조건 생성
    public static Predicate buildRecommendPagePredicate(ProjectReqDTO.RecommendProjectsPageReq req) {
        QProject project = QProject.project;
        QProjectRequirementTechstack techstack = QProjectRequirementTechstack.projectRequirementTechstack;

        BooleanBuilder builder = new BooleanBuilder();

        // 삭제되지 않은 프로젝트만
        builder.and(project.status.ne(ProjectStatus.DELETED));

        // 프로젝트 분야 필터
        if (req.projectField() != null) {
            builder.and(project.projectField.eq(req.projectField()));
        }

        // 카테고리 필터
        if (req.category() != null) {
            builder.and(project.category.genre.eq(req.category()));
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
        if (req.position() != null) {
            builder.and(project.requirements.any().part.eq(req.position()));
        }

        // 기술 스택 필터
        if (req.techstackName() != null) {
            builder.and(project.id.in(
                    com.querydsl.jpa.JPAExpressions
                            .select(techstack.requirement.project.id)
                            .from(techstack)
                            .where(techstack.techstack.name.eq(req.techstackName()))
            ));
        }

        return builder;
    }
}