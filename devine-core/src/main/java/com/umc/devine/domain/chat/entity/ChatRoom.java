package com.umc.devine.domain.chat.entity;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "chat_room")
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member1_id", nullable = false)
    private Member member1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member2_id", nullable = false)
    private Member member2;

    @Column(name = "member1_left", nullable = false)
    @Builder.Default
    private Boolean member1Left = false;

    @Column(name = "member2_left", nullable = false)
    @Builder.Default
    private Boolean member2Left = false;

    @Column(name = "member1_left_at")
    private LocalDateTime member1LeftAt;

    @Column(name = "member2_left_at")
    private LocalDateTime member2LeftAt;

    public void leave(Long memberId) {
        if (member1.getId().equals(memberId)) {
            this.member1Left = true;
            this.member1LeftAt = LocalDateTime.now();
        } else if (member2.getId().equals(memberId)) {
            this.member2Left = true;
            this.member2LeftAt = LocalDateTime.now();
        }
    }

    public void rejoin(Long memberId) {
        if (member1.getId().equals(memberId) && this.member1Left) {
            this.member1Left = false;
        } else if (member2.getId().equals(memberId) && this.member2Left) {
            this.member2Left = false;
        }
    }

    public boolean isActiveMember(Long memberId) {
        if (member1.getId().equals(memberId)) {
            return !this.member1Left;
        } else if (member2.getId().equals(memberId)) {
            return !this.member2Left;
        }
        return false;
    }

    public Member getOtherMember(Long memberId) {
        return member1.getId().equals(memberId) ? member2 : member1;
    }

    public boolean isBothLeft() {
        return this.member1Left && this.member2Left;
    }

    public LocalDateTime getLeftAt(Long memberId) {
        if (member1.getId().equals(memberId)) {
            return this.member1LeftAt;
        } else if (member2.getId().equals(memberId)) {
            return this.member2LeftAt;
        }
        return null;
    }
}
