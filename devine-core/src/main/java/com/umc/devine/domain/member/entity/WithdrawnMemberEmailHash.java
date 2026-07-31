package com.umc.devine.domain.member.entity;

import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 강제탈퇴 회원의 이메일 해시를 1년간 보관하는 재가입 제한 블랙리스트. 하드삭제 후에도 대조 가능하도록 Member FK 없이 보관한다. */
@Entity
@Table(name = "withdrawn_member_email_hash")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class WithdrawnMemberEmailHash extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawn_member_email_hash_id")
    private Long id;

    @Column(name = "email_hash", nullable = false, length = 64)
    private String emailHash;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static WithdrawnMemberEmailHash of(String emailHash, LocalDateTime withdrawnAt) {
        return WithdrawnMemberEmailHash.builder()
                .emailHash(emailHash)
                .withdrawnAt(withdrawnAt)
                .expiresAt(withdrawnAt.plusYears(1))
                .build();
    }
}
