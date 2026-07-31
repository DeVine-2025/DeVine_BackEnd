package com.umc.devine.domain.member.service.command;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.repository.ComplaintHistoryRepository;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberStatusHistory;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.enums.MemberStatusAction;
import com.umc.devine.domain.member.event.MemberWithdrawnEvent;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberStatusHistoryRepository;
import com.umc.devine.domain.ticket.entity.CreditRefundRequest;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.repository.CreditRefundRequestRepository;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.infrastructure.email.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberWithdrawalCommandServiceImpl implements MemberWithdrawalCommandService {

    private final MemberRepository memberRepository;
    private final ContactRepository contactRepository;
    private final MemberGithubDataCleanupService memberGithubDataCleanupService;
    private final MemberReportCreditRepository memberReportCreditRepository;
    private final CreditRefundRequestRepository creditRefundRequestRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    private final MemberStatusHistoryRepository memberStatusHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final List<MemberResDTO.WithdrawalDataScopeItemDTO> SELF_WITHDRAWAL_DATA_SCOPE = List.of(
            item("회원 프로필", "즉시 삭제"),
            item("GitHub 원본 연동 데이터", "즉시 삭제"),
            item("GitHub 익명화·벡터화 데이터", "보관 유지"),
            item("채팅 메시지", "발신자 익명 처리 (상대방 대화 내용은 유지)"),
            item("매칭 지원/제안 이력", "탈퇴 후 1년 보관 후 파기"),
            item("신고·제재 이력 (본인이 신고한 건)", "즉시 삭제"),
            item("결제·리포트 생성권 구매 내역", "5년 보관"),
            item("미사용 리포트 생성권", "환불 신청 시 환불, 미신청 시 소멸"),
            item("미사용 쿠폰", "즉시 소멸"),
            item("로그인 이력/접속 IP", "3개월 후 자동 파기")
    );

    private static MemberResDTO.WithdrawalDataScopeItemDTO item(String name, String treatment) {
        return MemberResDTO.WithdrawalDataScopeItemDTO.builder().item(name).treatment(treatment).build();
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResDTO.WithdrawalPreviewDTO getWithdrawalPreview(Member member) {
        int remainingCredits = memberReportCreditRepository.findByMember(member)
                .map(MemberReportCredit::getRemainingCount)
                .orElse(0);
        long couponCount = memberCouponRepository.countByMember(member);

        return MemberResDTO.WithdrawalPreviewDTO.builder()
                .remainingReportCredits(remainingCredits)
                .couponCount(couponCount)
                .dataScope(SELF_WITHDRAWAL_DATA_SCOPE)
                .build();
    }

    @Override
    public MemberResDTO.WithdrawalResultDTO selfWithdraw(Member member, MemberReqDTO.SelfWithdrawReq request) {
        // 동시 탈퇴 요청(중복 클릭)이 경쟁하지 않도록 행 잠금을 걸고 다시 조회한다.
        Member locked = memberRepository.findByIdForUpdate(member.getId())
                .orElseThrow(() -> new MemberException(MemberErrorReason.NOT_FOUND));

        requireWithdrawableStatus(locked);
        requireConfirmation(locked, request);

        int creditsForfeitedOrRefunded = handleReportCredits(locked, request.refundRequested());
        memberCouponRepository.bulkDeleteByMember(locked);
        deleteOwnComplaints(locked);

        String email = findEmail(locked);
        String originalClerkId = locked.getClerkId();

        locked.selfWithdraw();
        memberRepository.saveAndFlush(locked);

        memberGithubDataCleanupService.deleteGithubLinkedData(locked);
        recordAuditLog(locked);

        eventPublisher.publishEvent(MemberWithdrawnEvent.builder()
                .memberId(locked.getId())
                .originalClerkId(originalClerkId)
                .build());

        if (email != null) {
            eventPublisher.publishEvent(EmailNotificationEvent.builder()
                    .to(email)
                    .subject("[DeVine] 회원 탈퇴가 완료되었습니다")
                    .body(withdrawalEmailBody(request.refundRequested()))
                    .build());
        }

        return MemberResDTO.WithdrawalResultDTO.builder()
                .withdrawn(true)
                .refundRequestCreated(request.refundRequested() && creditsForfeitedOrRefunded > 0)
                .creditsForfeitedOrRefunded(creditsForfeitedOrRefunded)
                .withdrawnAt(locked.getDeletedAt())
                .build();
    }

    private void requireWithdrawableStatus(Member member) {
        if (member.getUsed() != MemberStatus.ACTIVE && member.getUsed() != MemberStatus.INACTIVE) {
            throw new MemberException(MemberErrorReason.INVALID_WITHDRAWAL_STATUS);
        }
    }

    private void requireConfirmation(Member member, MemberReqDTO.SelfWithdrawReq request) {
        // 신원 확인이 아니라 오탈퇴 방지용 의도 확인이라 trim만 적용하고 대소문자는 그대로 비교한다.
        String input = request.confirmationText() == null ? "" : request.confirmationText().trim();
        if (!member.getNickname().equals(input)) {
            throw new MemberException(MemberErrorReason.WITHDRAWAL_CONFIRMATION_MISMATCH);
        }
    }

    private int handleReportCredits(Member member, boolean refundRequested) {
        Optional<MemberReportCredit> creditOpt = memberReportCreditRepository.findByMember(member);
        if (creditOpt.isEmpty()) {
            return 0;
        }
        MemberReportCredit credit = creditOpt.get();
        int remaining = credit.getRemainingCount();
        if (remaining > 0 && refundRequested) {
            creditRefundRequestRepository.save(CreditRefundRequest.of(member, remaining));
        }
        credit.voidCredits();
        // 이후의 clearAutomatically=true 벌크 삭제가 영속성 컨텍스트를 비우기 전에 변경분을 DB에 반영해둔다.
        memberReportCreditRepository.saveAndFlush(credit);
        return remaining;
    }

    private void deleteOwnComplaints(Member member) {
        List<Complaint> ownComplaints = complaintRepository.findByComplainantId(member.getId());
        if (ownComplaints.isEmpty()) {
            return;
        }
        List<Long> complaintIds = ownComplaints.stream().map(Complaint::getId).toList();
        complaintHistoryRepository.bulkDeleteByComplaintIdIn(complaintIds);
        complaintRepository.bulkDeleteByComplainantId(member.getId());
    }

    private String findEmail(Member member) {
        return contactRepository.findAllByMember(member).stream()
                .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                .map(Contact::getValue)
                .findFirst()
                .orElse(null);
    }


    private void recordAuditLog(Member member) {
        memberStatusHistoryRepository.save(MemberStatusHistory.builder()
                .member(member)
                .action(MemberStatusAction.SELF_WITHDRAW)
                .status(member.getUsed())
                .build());
    }

    private String withdrawalEmailBody(boolean refundRequested) {
        if (refundRequested) {
            return "탈퇴가 완료되었습니다. 잔여 리포트 생성권 환불 신청이 접수되어 영업일 기준 3일 이내에 처리됩니다.";
        }
        return "탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.";
    }
}
