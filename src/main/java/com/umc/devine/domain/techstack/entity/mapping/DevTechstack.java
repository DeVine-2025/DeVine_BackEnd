package com.umc.devine.domain.techstack.entity.mapping;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "dev_techstack",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "techstack_id"}))
public class DevTechstack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "techstack_id", nullable = false)
    private Techstack techstack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TechstackSource source;
}
