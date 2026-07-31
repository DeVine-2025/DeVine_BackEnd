package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.service.command.MemberGithubDataCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** GitHub 원본 연동 데이터(report_embedding, dev_report, git_repo_url, dev_techstack, contact)는 이미 삭제 대상이라 감사 보존이 필요 없다. */
@Component
@Order(10)
@RequiredArgsConstructor
public class GithubDataHardDeleteHandler implements MemberHardDeleteHandler {

    private final MemberGithubDataCleanupService memberGithubDataCleanupService;

    @Override
    public void handle(Member member) {
        memberGithubDataCleanupService.deleteGithubLinkedData(member);
    }
}
