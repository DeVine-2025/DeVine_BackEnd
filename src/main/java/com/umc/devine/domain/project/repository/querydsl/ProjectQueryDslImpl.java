package com.umc.devine.domain.project.repository.querydsl;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.QProject;
import com.umc.devine.domain.project.enums.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectQueryDslImpl implements ProjectQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Project> searchProjects(Predicate predicate, Pageable pageable) {
        QProject project = QProject.project;

        JPAQuery<Project> query = queryFactory
                .selectFrom(project)
                .where(predicate)
                .orderBy(project.createdAt.desc());

        List<Project> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = query.fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Project> searchRecommendedProjects(Predicate predicate, Pageable pageable) {
        QProject project = QProject.project;

        // TODO: 추천 알고리즘 기반 정렬 추가
        // 현재는 생성일순으로 정렬
        JPAQuery<Project> query = queryFactory
                .selectFrom(project)
                .where(predicate)
                .orderBy(project.createdAt.desc());

        List<Project> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = query.fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Project> findAllActiveProjects(int limit) {
        QProject project = QProject.project;

        return queryFactory
                .selectFrom(project)
                .where(project.status.ne(ProjectStatus.DELETED))
                .orderBy(project.createdAt.desc())
                .limit(limit)
                .fetch();
    }
}