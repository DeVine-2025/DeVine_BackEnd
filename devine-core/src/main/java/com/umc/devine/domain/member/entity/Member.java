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
import java.util.UUID;


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
     * 회원 탈퇴 처리.
     * 상태를 DELETED로 바꾸고 탈퇴 시각을 기록한 뒤, 개인정보 컬럼을 익명화합니다.
     * <p>익명화 항목: clerkId(unique 충돌 회피용으로 deleted-{uuid}로 치환),
     * name, nickname, address, image, body, githubUsername.
     * <p>Clerk 사용자 삭제는 트랜잭션 커밋 후 이벤트 리스너에서 수행됩니다.
     */
    public void withdraw() {
        this.used = MemberStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.clerkId = "deleted-" + UUID.randomUUID();
        this.name = null;
        this.nickname = "deleted-" + UUID.randomUUID();
        this.address = null;
        this.image = null;
        this.body = null;
        this.githubUsername = null;
    }

}
