package com.umc.devine.admin.member.enums;

public enum MemberStatusAction {
    SUSPEND, UNSUSPEND, FORCE_WITHDRAW, CANCEL_WITHDRAWAL, WITHDRAWAL_FINALIZED,
    /** 회원 자진 탈퇴. 사유/개인정보 없이 액션과 시각만 남긴다. */
    SELF_WITHDRAW
}
