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

/**
 * GitHub 데이터 삭제 공통 함수(01-1 기능명세서 4장).
 * 회원 탈퇴와 GitHub 연동 해제(다음 핸드오프)에서 같이 쓴다.
 * DB만 바꾸고 외부 호출(GitHub 권한 회수 등)은 하지 않는다. 호출하는 쪽의 트랜잭션 안에서 실행된다.
 *
 * <p>리포트를 가리키는 북마크는 지울 것이 없다. 북마크 대상은 PROJECT, DEVELOPER뿐이다.
 */
@Component
@RequiredArgsConstructor
public class GithubDataCleanupHelper {

    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final DevReportRepository devReportRepository;
    private final ReportEmbeddingRepository reportEmbeddingRepository;
    private final DevTechstackRepository devTechstackRepository;

    public void deleteAll(Member member) {
        // 공개 리포트, 생성 중인 리포트 포함. FK 순서대로 임베딩 → 리포트 → 레포 순으로 지운다
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
