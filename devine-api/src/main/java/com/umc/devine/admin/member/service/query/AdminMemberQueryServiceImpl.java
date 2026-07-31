package com.umc.devine.admin.member.service.query;

import com.querydsl.core.BooleanBuilder;
import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.admin.member.exception.MemberAdminException;
import com.umc.devine.admin.member.exception.code.MemberAdminErrorReason;
import com.umc.devine.domain.member.repository.MemberLoginHistoryRepository;
import com.umc.devine.admin.member.converter.AdminMemberConverter;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.QContact;
import com.umc.devine.domain.member.entity.QMember;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberQueryServiceImpl implements AdminMemberQueryService {

    private final MemberRepository memberRepository;
    private final ContactRepository contactRepository;
    private final PaymentRepository paymentRepository;
    private final MemberLoginHistoryRepository memberLoginHistoryRepository;

    @Override
    public PagedResponse<AdminMemberResDTO.MemberSummaryDTO> getMemberList(AdminMemberReqDTO.SearchReq request) {
        BooleanBuilder predicate = buildPredicate(request);
        Page<Member> page = memberRepository.search(predicate, request.toPageable());

        Map<Long, String> emailsByMemberId = findEmailsByMembers(page.getContent());

        List<AdminMemberResDTO.MemberSummaryDTO> content = page.getContent().stream()
                .map(member -> AdminMemberConverter.toMemberSummaryDTO(member, emailsByMemberId.get(member.getId())))
                .toList();

        return PagedResponse.of(page, content);
    }

    private BooleanBuilder buildPredicate(AdminMemberReqDTO.SearchReq request) {
        QMember member = QMember.member;
        QContact contact = QContact.contact;
        BooleanBuilder builder = new BooleanBuilder();

        if (request.keyword() != null && !request.keyword().isBlank()) {
            String keyword = request.keyword();
            builder.and(member.nickname.containsIgnoreCase(keyword)
                    .or(member.name.containsIgnoreCase(keyword))
                    .or(contact.value.containsIgnoreCase(keyword)));
        }
        return builder;
    }

    private Map<Long, String> findEmailsByMembers(List<Member> members) {
        if (members.isEmpty()) {
            return Map.of();
        }
        return contactRepository.findAllByMemberIn(members).stream()
                .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                .collect(Collectors.toMap(contact -> contact.getMember().getId(), Contact::getValue, (a, b) -> a));
    }

    @Override
    public AdminMemberResDTO.MemberDetailRes getMemberDetail(String nickname) {
        Member member = memberRepository.findByNicknameIncludingInactive(nickname)
                .orElseThrow(() -> new MemberAdminException(MemberAdminErrorReason.MEMBER_NOT_FOUND));

        String email = contactRepository.findAllByMember(member).stream()
                .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                .map(Contact::getValue)
                .findFirst()
                .orElse(null);

        // TODO: 신고 이력은 [admin.complaint] 기능 병합 후 respondentHistory 연동 필요
        List<Payment> payments = paymentRepository.findAllByMemberWithTransactions(member);
        AdminMemberResDTO.PaymentSummaryDTO paymentSummary = AdminMemberConverter.toPaymentSummaryDTO(payments);

        List<AdminMemberResDTO.LoginHistoryDTO> loginHistory = memberLoginHistoryRepository
                .findTop10ByMemberIdOrderByLoginAtDesc(member.getId()).stream()
                .map(AdminMemberConverter::toLoginHistoryDTO)
                .toList();

        return AdminMemberConverter.toMemberDetailRes(member, email, paymentSummary, loginHistory);
    }
}
