package com.umc.devine.domain.member.entity;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "clerk_id", unique = true, length = 255)
    private String clerkId;

    @Column(nullable = true, length = 10)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(nullable = true, length = 255)
    @Builder.Default
    private String address = null;

    @Column(nullable = false)
    @Builder.Default
    private boolean disclosure = true;

    @Column(name = "proposal_alarm", nullable = false)
    @Builder.Default
    private boolean proposalAlarm = true;

    public boolean getDisclosure() {
        return disclosure;
    }

    public boolean getProposalAlarm() {
        return proposalAlarm;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "main_type", nullable = false)
    private MemberMainType mainType;

    @Column(nullable = true, length = 512)
    @Builder.Default
    private String image = null;

    @Column(nullable = true, length = 255)
    @Builder.Default
    private String body = null;

    @Column(name = "github_username", nullable = true, length = 39)
    @Builder.Default
    private String githubUsername = null;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus used;

    @Column(name = "scheduled_withdrawal_at")
    private LocalDateTime scheduledWithdrawalAt;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberCategory> memberCategories = new ArrayList<>();

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateImage(String image) {
        this.image = image;
    }

    public void updateAddress(String address) {
        this.address = address;
    }

    public void updateBody(String body) {
        this.body = body;
    }

    public void updateMainType(MemberMainType mainType) {
        this.mainType = mainType;
    }

    public void updateDisclosure(boolean disclosure) {
        this.disclosure = disclosure;
    }

    public void updateProposalAlarm(boolean proposalAlarm) {
        this.proposalAlarm = proposalAlarm;
    }

    public void updateGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public void clearCategories() {
        this.memberCategories.clear();
    }

    public void addCategories(List<Category> categories) {
        categories.forEach(category ->
            this.memberCategories.add(MemberCategory.builder()
                    .member(this)
                    .category(category)
                    .build())
        );
    }

    /**
     * 회원 탈퇴 처리를 위한 메서드
     * 현재는 주로 테스트 코드에서 회원의 상태를 변경하여 필터링 로직을 검증하는 용도로 사용됩니다.
     */
    public void withdraw() {
        this.used = MemberStatus.DELETED;
    }

    /**
     * 관리자에 의한 계정 정지.
     */
    public void suspend() {
        this.used = MemberStatus.SUSPENDED;
    }

    /**
     * 관리자에 의한 정지 해제.
     */
    public void unsuspend() {
        this.used = MemberStatus.ACTIVE;
    }

    /**
     * 관리자에 의한 강제탈퇴(자격상실) 예정 통지. 30일 소명 절차 후 스케줄러가 최종 확정한다.
     */
    public void scheduleForceWithdrawal(LocalDateTime scheduledWithdrawalAt) {
        this.used = MemberStatus.PENDING_WITHDRAWAL;
        this.scheduledWithdrawalAt = scheduledWithdrawalAt;
    }

    /**
     * 소명 성공 등으로 예정된 강제탈퇴를 취소.
     */
    public void cancelScheduledWithdrawal() {
        this.used = MemberStatus.ACTIVE;
        this.scheduledWithdrawalAt = null;
    }

    // 닉네임 규칙(한글, 영문, 숫자, -, _)이 허용하지 않는 '#'을 넣어 실제 회원 닉네임과 겹치지 않게 한다
    public static final String WITHDRAWN_NICKNAME_PREFIX = "deleted#";

    /**
     * 회원 자진 탈퇴(01-1 FUNC-MEM-003). 행은 결제/매칭 기록의 FK를 위해 남기고 개인정보만 비운다.
     * 닉네임은 NOT NULL UNIQUE라 비우지 않고 회원 ID 기반 자리표시값으로 바꾼다. 화면에는 "탈퇴한 회원"으로 보인다.
     * 예정된 강제탈퇴 확정도 함께 취소된다(상태가 DELETED라 확정 배치 대상에서 빠진다).
     */
    public void anonymizeForWithdrawal() {
        this.name = null;
        this.nickname = WITHDRAWN_NICKNAME_PREFIX + this.id;
        this.address = null;
        this.body = null;
        this.image = null;
        this.githubUsername = null;
        this.clerkId = null;
        this.scheduledWithdrawalAt = null;
        this.disclosure = false;
        this.proposalAlarm = false;
        this.memberCategories.clear();
        this.used = MemberStatus.DELETED;
    }

    public boolean isWithdrawn() {
        return this.used == MemberStatus.DELETED;
    }

    /** 결정 C 대상: 정지 중이거나 강제탈퇴(자격상실) 예정인 회원(01-1 FUNC-MEM-006). */
    public boolean isSanctioned() {
        return this.used == MemberStatus.SUSPENDED || this.scheduledWithdrawalAt != null;
    }

    /** 남은 기록(채팅, 지원 현황, 알림)에 보여 줄 탈퇴 회원 이름(01-1 FUNC-MEM-008). */
    public static final String WITHDRAWN_DISPLAY_NAME = "탈퇴한 회원";

    /** 다른 회원에게 보여 줄 닉네임. 탈퇴했으면 자리표시값 대신 "탈퇴한 회원"이다. */
    public String getDisplayNickname() {
        return isWithdrawn() ? WITHDRAWN_DISPLAY_NAME : this.nickname;
    }

    /** 다른 회원에게 보여 줄 프로필 이미지. 탈퇴했으면 null(기본 이미지)이다. */
    public String getDisplayImage() {
        return isWithdrawn() ? null : this.image;
    }

    /**
     * 30일 소명 절차 만료 후 스케줄러가 호출하는 최종 탈퇴 확정.
     */
    public void finalizeWithdrawal() {
        this.used = MemberStatus.DELETED;
        this.scheduledWithdrawalAt = null;
    }

}
