package com.umc.devine.domain.member.entity;

import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = true, length = 255)
    @Builder.Default
    private String address = null;

    @Column(nullable = false)
    private Boolean disclosure;

    @Enumerated(EnumType.STRING)
    @Column(name = "main_type", nullable = false)
    private MemberMainType mainType;

    @Column(nullable = true, length = 255)
    @Builder.Default
    private String image = null;

    @Column(nullable = true, length = 255)
    @Builder.Default
    private String body = null;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus used;

    @OneToMany(mappedBy = "member")
    @Builder.Default
    private List<Login> loginList = new ArrayList<>();

}
