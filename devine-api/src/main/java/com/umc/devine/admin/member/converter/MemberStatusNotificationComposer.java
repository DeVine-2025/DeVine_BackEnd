package com.umc.devine.admin.member.converter;

import java.time.LocalDateTime;

// 상태 전이 로직(AdminMemberCommandServiceImpl)과 알림 문구를 분리한다.
public class MemberStatusNotificationComposer {

    public record EmailContent(String subject, String body) {}

    public static EmailContent suspended(String reason) {
        return new EmailContent(
                "[DeVine] 계정이 정지되었습니다",
                "정지 사유: %s".formatted(reason)
        );
    }

    public static EmailContent unsuspended() {
        return new EmailContent(
                "[DeVine] 계정 정지가 해제되었습니다",
                "계정 정지가 해제되어 정상적으로 이용하실 수 있습니다."
        );
    }

    public static EmailContent forceWithdrawScheduled(String reason, LocalDateTime scheduledWithdrawalAt) {
        return new EmailContent(
                "[DeVine] 계정 자격상실(강제탈퇴) 예정 안내",
                """
                자격상실 사유: %s

                %s까지 소명하지 않으면 계정이 최종 탈퇴 처리됩니다.
                문의사항이 있으시면 고객센터로 연락해주세요.
                """.formatted(reason, scheduledWithdrawalAt)
        );
    }

    public static EmailContent withdrawalCancelled() {
        return new EmailContent(
                "[DeVine] 계정 자격상실(강제탈퇴) 예정이 취소되었습니다",
                "소명이 확인되어 예정되어 있던 강제탈퇴 처리가 취소되었습니다. 계정을 정상적으로 이용하실 수 있습니다."
        );
    }
}
