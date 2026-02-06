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

        // 프로젝트 분야 필터 (ALL이면 전체 조회이므로 필터 미적용)
        if (req.projectField() != null && req.projectField() != ProjectField.ALL) {
            builder.and(project.projectField.eq(req.projectField()));
        }

        // 카테고리 필터 (ALL이면 전체 조회이므로 필터 미적용)
        if (req.category() != null && req.category() != CategoryGenre.ALL) {
            builder.and(project.category.genre.eq(req.category()));
        }

        // 진행 기간 필터
        if (req.durationRange() != null) {
            builder.and(project.durationRange.eq(req.durationRange()));
        }

        // 포지션 필터 (ALL이면 전체 조회이므로 필터 미적용)
        if (req.position() != null && req.position() != ProjectPart.ALL) {
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

        // 프로젝트 분야 필터 (ALL이면 전체 조회이므로 필터 미적용)
        if (req.projectField() != null && req.projectField() != ProjectField.ALL) {
            builder.and(project.projectField.eq(req.projectField()));
        }

        // 카테고리 필터 (ALL이면 전체 조회이므로 필터 미적용)
        if (req.category() != null && req.category() != CategoryGenre.ALL) {
            builder.and(project.category.genre.eq(req.category()));
        }

        // 진행 기간 필터
        if (req.durationRange() != null) {
            builder.and(project.durationRange.eq(req.durationRange()));
        }

        // 포지션 필터 (ALL이면 전체 조회이므로 필터 미적용)
        if (req.position() != null && req.position() != ProjectPart.ALL) {
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