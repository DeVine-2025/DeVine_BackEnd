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

    /** 탈퇴(자진/강제 확정) 시각. Hard Delete 배치가 유예기간 경과 여부를 판단하는 기준이 된다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    /**
     * 30일 소명 절차 만료 후 스케줄러가 호출하는 최종 탈퇴 확정.
     */
    public void finalizeWithdrawal() {
        this.used = MemberStatus.DELETED;
        this.scheduledWithdrawalAt = null;
    }

    /**
     * 회원 자진 탈퇴. 강제탈퇴(30일 소명 절차)와 달리 즉시 확정되며, PII를 익명화한다.
     */
    public void selfWithdraw() {
        this.used = MemberStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        anonymizePersonalInfo();
    }

    /** clerkId/nickname은 unique 제약을 회피하기 위해 고유한 값으로 대체한다. */
    private void anonymizePersonalInfo() {
        this.clerkId = "deleted-" + java.util.UUID.randomUUID();
        this.nickname = "deleted-" + this.id;
        this.name = null;
        this.address = null;
        this.image = null;
        this.body = null;
        this.githubUsername = null;
    }

}
