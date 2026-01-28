package com.umc.devine.domain.member.entity;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public void disclosure(Boolean disclosure) {
        this.disclosure = disclosure;
    }

    public void updateProfile(MemberReqDTO.UpdateMemberDTO dto) {
        Optional.ofNullable(dto.nickname()).ifPresent(this::updateNickname);
        Optional.ofNullable(dto.imageUrl()).ifPresent(this::updateImage);
        Optional.ofNullable(dto.address()).ifPresent(this::updateAddress);
        Optional.ofNullable(dto.body()).ifPresent(this::updateBody);
        Optional.ofNullable(dto.mainType()).ifPresent(this::updateMainType);
        Optional.ofNullable(dto.disclosure()).ifPresent(this::disclosure);
    }
}
