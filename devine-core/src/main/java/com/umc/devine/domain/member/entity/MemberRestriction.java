package com.umc.devine.domain.member.entity;

import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "member_restriction")
public class MemberRestriction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_restriction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "sanction_summary", columnDefinition = "TEXT")
    private String sanctionSummary;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
