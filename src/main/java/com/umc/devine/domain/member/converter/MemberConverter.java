package com.umc.devine.domain.member.converter;

import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;

public class MemberConverter {
    public static MemberResDTO.MemberDetailDTO toMemberDetailDTO(Member member) {
        return MemberResDTO.MemberDetailDTO.builder()
                .name(member.getName())
                .nickname(member.getNickname())
                .address(member.getAddress())
                .disclosure(member.getDisclosure())
                .mainType(member.getMainType())
                .image(member.getImage())
                .body(member.getBody())
                .used(member.getUsed())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
