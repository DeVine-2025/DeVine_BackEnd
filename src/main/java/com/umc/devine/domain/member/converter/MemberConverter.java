package com.umc.devine.domain.member.converter;

import java.util.List;

import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;

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

    public static MemberResDTO.UserProfileDTO toUserProfileDTO(Member member, java.util.List<DevTechstack> devTechstacks) {
        List<String> techstackNames = devTechstacks.stream()
                .map(devTechstack -> devTechstack.getTechstack().getName().toString())
                .collect(java.util.stream.Collectors.toList());

        List<String> techGenres = devTechstacks.stream()
                .map(devTechstack -> devTechstack.getTechstack().getGenre().toString())
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        return MemberResDTO.UserProfileDTO.builder()
                .nickname(member.getNickname())
                .address(member.getAddress())
                .image(member.getImage())
                .body(member.getBody())
                .techstacks(techstackNames)
                .techGenres(techGenres)
                .build();
    }
}
