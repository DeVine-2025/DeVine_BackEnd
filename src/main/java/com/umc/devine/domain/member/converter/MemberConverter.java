package com.umc.devine.domain.member.converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Contact;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberAgreement;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.global.util.GitUrlParser;
import com.umc.devine.domain.member.enums.ContactType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.global.security.ClerkPrincipal;

public class MemberConverter {

    //회원가입 관련 Converter

    public static Member toMember(MemberReqDTO.SignupDTO dto, ClerkPrincipal principal) {
        return Member.builder()
                .clerkId(principal.getClerkId())
                .name(principal.getFullName())
                .nickname(dto.nickname())
                .image(dto.imageUrl())
                .body(dto.body())
                .mainType(dto.mainType())
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build();
    }

    public static MemberAgreement toMemberAgreement(Member member, Terms terms, Boolean agreed) {
        return MemberAgreement.builder()
                .member(member)
                .terms(terms)
                .agreed(agreed)
                .build();
    }

    public static List<MemberAgreement> toMemberAgreements(
            Member member,
            List<Terms> termsList,
            List<MemberReqDTO.AgreementDTO> agreementDTOs
    ) {
        return agreementDTOs.stream()
                .map(dto -> {
                    Terms terms = termsList.stream()
                            .filter(t -> t.getId().equals(dto.termsId()))
                            .findFirst()
                            .orElseThrow(() -> new MemberException(MemberErrorCode.TERMS_NOT_FOUND));
                    return toMemberAgreement(member, terms, dto.agreed());
                })
                .collect(Collectors.toList());
    }

    public static MemberCategory toMemberCategory(Member member, Category category) {
        return MemberCategory.builder()
                .member(member)
                .category(category)
                .build();
    }

    public static List<MemberCategory> toMemberCategories(Member member, List<Category> categories) {
        return categories.stream()
                .map(category -> toMemberCategory(member, category))
                .collect(Collectors.toList());
    }

    public static DevTechstack toDevTechstack(Member member, Techstack techstack) {
        return DevTechstack.builder()
                .member(member)
                .techstack(techstack)
                .source(TechstackSource.MANUAL)
                .build();
    }

    public static List<DevTechstack> toDevTechstacks(Member member, List<Techstack> techstacks) {
        return techstacks.stream()
                .map(techstack -> toDevTechstack(member, techstack))
                .collect(Collectors.toList());
    }

    public static Contact toSignupContact(Member member, ContactType type, String value) {
        return Contact.builder()
                .member(member)
                .contactType(type)
                .value(value)
                .build();
    }

    public static MemberResDTO.SignupResultDTO toSignupResultDTO(Member member) {
        return MemberResDTO.SignupResultDTO.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .mainType(member.getMainType())
                .build();
    }

    public static MemberResDTO.TermsDTO toTermsDTO(Terms terms) {
        return MemberResDTO.TermsDTO.builder()
                .termsId(terms.getId())
                .title(terms.getTitle())
                .content(terms.getContent())
                .required(terms.getRequired())
                .build();
    }

    public static MemberResDTO.TermsListDTO toTermsListDTO(List<Terms> termsList) {
        List<MemberResDTO.TermsDTO> termsDTOs = termsList.stream()
                .map(MemberConverter::toTermsDTO)
                .collect(Collectors.toList());

        return MemberResDTO.TermsListDTO.builder()
                .terms(termsDTOs)
                .build();
    }

    public static Contact toContact(Member member, MemberReqDTO.ContactDTO dto) {
        return Contact.builder()
                .member(member)
                .contactType(dto.type())
                .value(dto.value())
                .link(dto.link())
                .build();
    }

    public static List<Contact> toContacts(Member member, MemberReqDTO.ContactDTO[] dtos) {
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

    public static MemberResDTO.DeveloperDTO toDeveloperDTO(Member member, List<DevTechstack> devTechstacks) {
        List<String> techstackNames = devTechstacks.stream()
                .map(dt -> dt.getTechstack().getName().toString())
                .collect(Collectors.toList());

        return MemberResDTO.DeveloperDTO.builder()
                .nickname(member.getNickname())
                .image(member.getImage())
                .body(member.getBody())
                .techstacks(techstackNames)
                .build();
    }

    public static MemberResDTO.GitRepoDTO toGitRepoDTO(GitRepoUrl gitRepoUrl) {
        return MemberResDTO.GitRepoDTO.builder()
                .gitRepoId(gitRepoUrl.getId())
                .name(GitUrlParser.extractRepoName(gitRepoUrl.getGitUrl()))
                .gitUrl(gitRepoUrl.getGitUrl())
                .description(gitRepoUrl.getGitDescription())
                .build();
    }

    public static MemberResDTO.GitRepoListDTO toGitRepoListDTO(List<GitRepoUrl> gitRepoUrls) {
        List<MemberResDTO.GitRepoDTO> repos = gitRepoUrls.stream()
                .map(MemberConverter::toGitRepoDTO)
                .collect(Collectors.toList());

        return MemberResDTO.GitRepoListDTO.builder()
                .repos(repos)
                .build();
    }
}
