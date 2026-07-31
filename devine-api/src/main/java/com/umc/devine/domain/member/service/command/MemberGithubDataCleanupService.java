package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.report.repository.ReportEmbeddingRepository;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 자진/강제탈퇴 양쪽에서 공통으로 쓰는 GitHub 원본 연동 데이터 즉시 삭제 로직. */
@Component
@RequiredArgsConstructor
public class MemberGithubDataCleanupService {

    private final ReportEmbeddingRepository reportEmbeddingRepository;
    private final DevReportRepository devReportRepository;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final DevTechstackRepository devTechstackRepository;
    private final ContactRepository contactRepository;

    /** FK 체인 순서: report_embedding → dev_report → git_repo_url */
    public void deleteGithubLinkedData(Member member) {
        reportEmbeddingRepository.bulkDeleteByMember(member);
        devReportRepository.bulkDeleteByMember(member);
        gitRepoUrlRepository.bulkDeleteByMember(member);
        devTechstackRepository.bulkDeleteByMember(member);
        contactRepository.deleteAllByMember(member);
    }
}
