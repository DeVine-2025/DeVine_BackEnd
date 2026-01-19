package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.converter.MemberConverter;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService {

    private final MemberRepository memberRepository;

    @Override
    public MemberResDTO.MemberDetailDTO updateMember(Long memberId, MemberReqDTO.UpdateMemberDTO dto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        if (dto.nickname() != null) {
            member.updateNickname(dto.nickname());
        }
        if (dto.profileImageUrl() != null) {
            member.updateImage(dto.profileImageUrl());
        }
        if (dto.address() != null) {
            member.updateAddress(dto.address());
        }
        if (dto.body() != null) {
            member.updateBody(dto.body());
        }

        return MemberConverter.toMemberDetailDTO(member);
    }
}
