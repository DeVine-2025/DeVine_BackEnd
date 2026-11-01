package com.umc.devine.domain.project.helper;

import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class ProjectLifecycleHelper {

    // 마감일은 한국 날짜 기준이다. 서버 JVM 시간대와 무관하게 판단하도록 고정한다.
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MatchingRepository matchingRepository;

    public static LocalDate today() {
        return LocalDate.now(SERVICE_ZONE);
    }

    // 대기 건: PM이 수락·거절하지 않은 지원 + 개발자가 답하지 않은 제안
    public long countPending(Project project) {
        return matchingRepository.countByProjectAndStatus(project, MatchingStatus.PENDING);
    }

    // 마감 확인 훅: 대기 건 상태가 바뀌는 모든 경로에서 호출한다. 빠진 경로는 자동 마감 배치가 다음 날 정리한다.
    public void closeIfDeadlinePassedAndNoPending(Project project) {
        if (project.getStatus() == ProjectStatus.DEADLINE_PASSED && countPending(project) == 0) {
            project.close();
        }
    }
}
