package com.umc.devine.domain.member.converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;

public class MemberConverter {

    public static Contact toContact(Member member, MemberResDTO.ContactDTO dto) {
        return Contact.builder()
                .member(member)
                .contactType(dto.type())
                .value(dto.value())
                .link(dto.link())
                .build();
    }

    public static List<Contact> toContacts(Member member, MemberResDTO.ContactDTO[] dtos) {
        return Arrays.stream(dtos)
                .map(dto -> toContact(member, dto))
                .collect(Collectors.toList());
    }

    public static MemberResDTO.MemberProfileDTO toMemberProfileDTO(
            Member member,
            List<MemberCategory> memberCategories,
            List<Contact> contacts
    ) {
        MemberResDTO.MemberDetailDTO memberDTO = MemberResDTO.MemberDetailDTO.builder()
                .name(member.getName())
                .nickname(member.getNickname())
                .address(member.getAddress())
                .imageUrl(member.getImage())
                .disclosure(member.getDisclosure())
                .mainType(member.getMainType())
                .body(member.getBody())
                .used(member.getUsed())
                .createdAt(member.getCreatedAt())
                .build();

        List<CategoryGenre> domains = memberCategories.stream()
                .map(mc -> mc.getCategory().getGenre())
                .collect(Collectors.toList());

        List<MemberResDTO.ContactDTO> contactDTOs = contacts.stream()
                .map(contact -> MemberResDTO.ContactDTO.builder()
                        .type(contact.getContactType())
                        .value(contact.getValue())
                        .link(contact.getLink())
                        .build())
                .collect(Collectors.toList());

        return MemberResDTO.MemberProfileDTO.builder()
                .member(memberDTO)
                .domains(domains)
                .contacts(contactDTOs)
                .build();
    }

    public static MemberResDTO.MemberDetailDTO toMemberDetailDTO(Member member) {
        return MemberResDTO.MemberDetailDTO.builder()
                .name(member.getName())
                .nickname(member.getNickname())
                .address(member.getAddress())
                .disclosure(member.getDisclosure())
                .mainType(member.getMainType())
                .imageUrl(member.getImage())
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
