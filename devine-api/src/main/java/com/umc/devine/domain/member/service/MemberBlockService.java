package com.umc.devine.domain.member.service;

import com.umc.devine.domain.member.dto.MemberBlockResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberBlock;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.repository.MemberBlockRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.helper.MatchingNotificationHelper;
import com.umc.devine.domain.project.helper.ProjectLifecycleHelper;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBlockService {
    private final MemberRepository memberRepository;
    private final MemberBlockRepository blockRepository;
    private final MatchingRepository matchingRepository;
    private final ProjectRepository projectRepository;
    private final MatchingNotificationHelper notificationHelper;
    private final ProjectLifecycleHelper lifecycleHelper;

    public MemberBlockResDTO.Confirmation confirmation(Member member, String nickname) {
        Member target = target(member, nickname);
        return new MemberBlockResDTO.Confirmation(target.getNickname(),
                matchingRepository.countPendingBetween(member.getId(), target.getId()),
                "로그인하지 않고 볼 수 있는 공개 정보는 차단 후에도 볼 수 있어요. 취소된 지원·제안은 차단을 해제해도 복구되지 않아요.");
    }

    @Transactional
    public void block(Member member, String nickname) {
        Member target = target(member, nickname);
        memberRepository.lockPair(member.getId(), target.getId());
        if (blockRepository.existsByBlockerIdAndTargetId(member.getId(), target.getId())) {
            return;
        }
        blockRepository.save(MemberBlock.builder().blocker(member).target(target).build());

        for (Long projectId : matchingRepository.findPendingProjectIdsBetween(member.getId(), target.getId())) {
            Project project = projectRepository.findByIdForUpdate(projectId).orElseThrow();
            for (Matching matching : matchingRepository.findPendingBetweenInProject(projectId, member.getId(), target.getId())) {
                matching.cancel();
                notificationHelper.notifyCancelled(matching, member.getId());
            }
            lifecycleHelper.closeIfDeadlinePassedAndNoPending(project);
        }
    }

    public PagedResponse<MemberBlockResDTO.BlockedMember> list(Member member, Pageable pageable) {
        var page = blockRepository.findByBlockerId(member.getId(), pageable);
        return PagedResponse.of(page, page.getContent().stream()
                .map(block -> new MemberBlockResDTO.BlockedMember(block.getTarget().getNickname(),
                        block.getTarget().getImage(), block.getCreatedAt())).toList());
    }

    @Transactional
    public void unblock(Member member, String nickname) {
        memberRepository.findByNicknameIncludingInactive(nickname).ifPresent(target -> {
            memberRepository.lockPair(member.getId(), target.getId());
            blockRepository.deleteByBlockerIdAndTargetId(member.getId(), target.getId());
        });
    }

    private Member target(Member member, String nickname) {
        Member target = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorReason.NOT_FOUND));
        if (target.getId().equals(member.getId())) {
            throw new MemberException(MemberErrorReason.CANNOT_BLOCK_SELF);
        }
        return target;
    }
}
