package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.helper.PendingMatchingHelper;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.helper.MatchingNotificationHelper;
import com.umc.devine.domain.project.helper.ProjectLifecycleHelper;
import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberWithdrawalCommandServiceImpl implements MemberWithdrawalCommandService {

    private final PendingMatchingHelper pendingMatchingHelper;
    private final ProjectRepository projectRepository;
    private final MatchingNotificationHelper notificationHelper;
    private final ProjectLifecycleHelper lifecycleHelper;

    @Override
    public WithdrawalResDTO.CancelPendingResultDTO cancelPendingMatchings(Member member) {
        // 매칭 처리·마감과 같은 순서로 프로젝트를 먼저 잠근다. ID 순으로 잠가 교착을 피한다.
        pendingMatchingHelper.findPendingProjectIds(member).stream()
                .sorted()
                .forEach(projectRepository::findByIdForUpdate);

        // 잠근 뒤 다시 읽어, 그사이 상대가 수락·거절한 건은 건드리지 않는다
        List<Matching> pending = pendingMatchingHelper.findPending(member);

        Map<Long, Project> touchedProjects = new LinkedHashMap<>();
        for (Matching matching : pending) {
            matching.cancel();
            notificationHelper.notifyPendingCancelled(matching, member, PendingMatchingHelper.counterpartOf(matching, member));
            touchedProjects.putIfAbsent(matching.getProject().getId(), matching.getProject());
        }

        // 03 마감 확인 훅: 마감일 지남 상태에서 대기 건이 0이 되면 마감한다
        touchedProjects.values().forEach(lifecycleHelper::closeIfDeadlinePassedAndNoPending);

        return WithdrawalResDTO.CancelPendingResultDTO.builder()
                .canceledCount(pending.size())
                .build();
    }
}
