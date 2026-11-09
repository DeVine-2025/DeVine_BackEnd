package com.umc.devine.domain.member.helper;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.report.repository.ReportEmbeddingRepository;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GithubDataCleanupHelper {

    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final DevReportRepository devReportRepository;
    private final ReportEmbeddingRepository reportEmbeddingRepository;
    private final DevTechstackRepository devTechstackRepository;

    public void deleteAll(Member member) {
        List<Long> reportIds = devReportRepository.findIdsByMember(member);
        if (!reportIds.isEmpty()) {
            reportEmbeddingRepository.deleteAllByDevReportIdIn(reportIds);
            devReportRepository.deleteAllByIdIn(reportIds);
        }
        gitRepoUrlRepository.deleteAllByMemberBulk(member);

        devTechstackRepository.deleteAllByMemberAndSource(member, TechstackSource.AUTO);

        member.updateGithubUsername(null);
    }
}
