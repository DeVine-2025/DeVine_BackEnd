package com.umc.devine.admin.member.converter;

import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.domain.member.entity.MemberLoginHistory;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.entity.Payment;

import java.util.List;

public class AdminMemberConverter {

    public static AdminMemberResDTO.MemberSummaryDTO toMemberSummaryDTO(Member member, String email) {
        return AdminMemberResDTO.MemberSummaryDTO.builder()
                .name(member.getName())
                .nickname(member.getNickname())
                .email(email)
                .status(member.getUsed())
                .createdAt(member.getCreatedAt())
                .build();
    }

    public static AdminMemberResDTO.PaymentDTO toPaymentDTO(Payment payment) {
        return AdminMemberResDTO.PaymentDTO.builder()
                .paymentId(payment.getId())
                .orderName(payment.getOrderName())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public static AdminMemberResDTO.PaymentSummaryDTO toPaymentSummaryDTO(List<Payment> payments) {
        long totalAmount = payments.stream().mapToLong(Payment::getAmount).sum();
        List<AdminMemberResDTO.PaymentDTO> recentPayments = payments.stream()
                .limit(5)
                .map(AdminMemberConverter::toPaymentDTO)
                .toList();

        return AdminMemberResDTO.PaymentSummaryDTO.builder()
                .totalCount(payments.size())
                .totalAmount(totalAmount)
                .recentPayments(recentPayments)
                .build();
    }

    public static AdminMemberResDTO.LoginHistoryDTO toLoginHistoryDTO(MemberLoginHistory history) {
        return AdminMemberResDTO.LoginHistoryDTO.builder()
                .loginAt(history.getLoginAt())
                .build();
    }

    public static AdminMemberResDTO.MemberDetailRes toMemberDetailRes(
            Member member,
            String email,
            AdminMemberResDTO.PaymentSummaryDTO paymentSummary,
            List<AdminMemberResDTO.LoginHistoryDTO> loginHistory,
            ComplaintResDTO.RespondentHistoryRes respondentHistory
    ) {
        return AdminMemberResDTO.MemberDetailRes.builder()
                .name(member.getName())
                .nickname(member.getNickname())
                .email(email)
                .mainType(member.getMainType())
                .status(member.getUsed())
                .scheduledWithdrawalAt(member.getScheduledWithdrawalAt())
                .createdAt(member.getCreatedAt())
                .paymentSummary(paymentSummary)
                .loginHistory(loginHistory)
                .respondentHistory(respondentHistory)
                .build();
    }

    public static AdminMemberResDTO.ChangeStatusRes toChangeStatusRes(Member member) {
        return AdminMemberResDTO.ChangeStatusRes.builder()
                .nickname(member.getNickname())
                .status(member.getUsed())
                .scheduledWithdrawalAt(member.getScheduledWithdrawalAt())
                .build();
    }
}
