package com.umc.devine.domain.project.repository.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.project.entity.QProject;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.techstack.entity.mapping.QProjectRequirementTechstack;
import com.umc.devine.domain.techstack.enums.TechName;

import com.querydsl.core.types.dsl.DateExpression;

import java.time.LocalDate;
import java.util.List;

public class ProjectPredicateBuilder {

    // 프로젝트 검색 조건 생성
    public static Predicate buildSearchPredicate(
            List<ProjectField> projectFields,
            List<CategoryGenre> categories,
            List<DurationRange> durationRanges,
            List<ProjectPart> positions,
            List<TechName> techstackNames
    ) {
        QProject project = QProject.project;
        QProjectRequirementTechstack techstack = QProjectRequirementTechstack.projectRequirementTechstack;

        BooleanBuilder builder = new BooleanBuilder();

        // 모집 중인 프로젝트만
        builder.and(project.status.eq(ProjectStatus.RECRUITING));

        // 모집마감일이 지나지 않은 프로젝트만
        builder.and(project.recruitmentDeadline.goe(DateExpression.currentDate(LocalDate.class)));

        // 프로젝트 분야 필터 (비어있으면 전체 조회)
        if (projectFields != null && !projectFields.isEmpty()) {
            builder.and(project.projectField.in(projectFields));
        }

        // 카테고리 필터 (비어있으면 전체 조회)
        if (categories != null && !categories.isEmpty()) {
            builder.and(project.category.genre.in(categories));
        }

        // 진행 기간 필터 (복수 선택 가능)
        if (durationRanges != null && !durationRanges.isEmpty()) {
            builder.and(project.durationRange.in(durationRanges));
        }

        // 포지션 필터 (비어있으면 전체 조회)
        if (positions != null && !positions.isEmpty()) {
            builder.and(project.requirements.any().part.in(positions));
        }

        // 기술 스택 필터 (복수 선택 가능)
        if (techstackNames != null && !techstackNames.isEmpty()) {
            builder.and(project.id.in(
                    com.querydsl.jpa.JPAExpressions
                            .select(techstack.requirement.project.id)
                            .from(techstack)
                            .where(techstack.techstack.name.in(techstackNames))
            ));
        }

        return builder;
    }

}
