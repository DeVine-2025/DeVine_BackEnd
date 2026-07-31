package com.umc.devine.admin.member.service.command;

import com.umc.devine.admin.member.converter.AdminMemberConverter;
import com.umc.devine.admin.member.converter.MemberStatusNotificationComposer;
import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.admin.member.entity.MemberStatusHistory;
import com.umc.devine.admin.member.exception.MemberAdminException;
import com.umc.devine.admin.member.exception.code.MemberAdminErrorReason;
import com.umc.devine.admin.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.infrastructure.email.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminMemberCommandServiceImpl implements AdminMemberCommandService {

    private static final int FORCE_WITHDRAWAL_GRACE_DAYS = 30;

    private final MemberRepository memberRepository;
    private final MemberStatusHistoryRepository memberStatusHistoryRepository;
    private final ContactRepository contactRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public AdminMemberResDTO.ChangeStatusRes changeStatus(String nickname, String processorClerkId, AdminMemberReqDTO.ChangeStatusReq request) {
        Member member = memberRepository.findByNicknameIncludingInactive(nickname)
                .orElseThrow(() -> new MemberAdminException(MemberAdminErrorReason.MEMBER_NOT_FOUND));

        applyAction(member, request);

        Member processor = processorClerkId != null ? memberRepository.findByClerkId(processorClerkId).orElse(null) : null;

        memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                .member(member)
                .action(request.action())
                .status(member.getUsed())
                .reason(request.reason())
                .notifyRequested(Boolean.TRUE.equals(request.notifyRequested()))
                .scheduledWithdrawalAt(member.getScheduledWithdrawalAt())
                .processor(processor)
                .build());

        return AdminMemberConverter.toChangeStatusRes(member);
    }

    private void applyAction(Member member, AdminMemberReqDTO.ChangeStatusReq request) {
        switch (request.action()) {
            case SUSPEND -> {
                requireStatus(member, MemberStatus.ACTIVE, MemberStatus.INACTIVE);
                requireReason(request);
                member.suspend();
                if (Boolean.TRUE.equals(request.notifyRequested())) {
                    notifySuspended(member, request.reason());
                }
            }
            case UNSUSPEND -> {
                requireStatus(member, MemberStatus.SUSPENDED);
                member.unsuspend();
                notifyUnsuspended(member);
            }
            case FORCE_WITHDRAW -> {
                requireStatus(member, MemberStatus.ACTIVE, MemberStatus.INACTIVE);
                requireReason(request);
                member.scheduleForceWithdrawal(LocalDateTime.now().plusDays(FORCE_WITHDRAWAL_GRACE_DAYS));
                notifyForceWithdrawScheduled(member, request.reason());
            }
            case CANCEL_WITHDRAWAL -> {
                requireStatus(member, MemberStatus.PENDING_WITHDRAWAL);
                member.cancelScheduledWithdrawal();
                notifyWithdrawalCancelled(member);
            }
        }
    }

    private void requireStatus(Member member, MemberStatus... allowed) {
        for (MemberStatus status : allowed) {
            if (member.getUsed() == status) {
                return;
            }
        }
        if (member.getUsed() == MemberStatus.DELETED) {
            throw new MemberAdminException(MemberAdminErrorReason.ALREADY_WITHDRAWN);
        }
        throw new MemberAdminException(MemberAdminErrorReason.INVALID_STATUS_TRANSITION);
    }

    private void requireReason(AdminMemberReqDTO.ChangeStatusReq request) {
        if (!StringUtils.hasText(request.reason())) {
            throw new MemberAdminException(MemberAdminErrorReason.REASON_REQUIRED);
        }
    }

    private void notifySuspended(Member member, String reason) {
        notify(member, MemberStatusNotificationComposer.suspended(reason));
    }

    private void notifyUnsuspended(Member member) {
        notify(member, MemberStatusNotificationComposer.unsuspended());
    }

    private void notifyWithdrawalCancelled(Member member) {
        notify(member, MemberStatusNotificationComposer.withdrawalCancelled());
    }

    private void notifyForceWithdrawScheduled(Member member, String reason) {
        notify(member, MemberStatusNotificationComposer.forceWithdrawScheduled(reason, member.getScheduledWithdrawalAt()));
    }

    private void notify(Member member, MemberStatusNotificationComposer.EmailContent content) {
        findEmail(member).ifPresent(email -> eventPublisher.publishEvent(EmailNotificationEvent.builder()
                .to(email)
                .subject(content.subject())
                .body(content.body())
                .build()));
    }

    private Optional<String> findEmail(Member member) {
        return contactRepository.findAllByMember(member).stream()
                .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                .map(Contact::getValue)
                .findFirst();
    }
}
