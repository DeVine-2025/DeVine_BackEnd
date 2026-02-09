package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.auth.ClerkPrincipal;

public interface MemberCommandService {
    MemberResDTO.SignupResultDTO signup(ClerkPrincipal principal, MemberReqDTO.SignupDTO dto);
    MemberResDTO.MemberProfileDTO updateMember(Member member, MemberReqDTO.UpdateMemberDTO dto);
    TechstackResDTO.DevTechstackListDTO addMemberTechstacks(Member member, MemberReqDTO.AddTechstackDTO dto);
    TechstackResDTO.DevTechstackListDTO removeMemberTechstacks(Member member, MemberReqDTO.RemoveTechstackDTO dto);

    /**
     * 회원 탈퇴(논리 삭제)를 처리합니다.
     * 테스트 코드에서 활성/비활성 상태에 따른 필터링 검증을 위해 사용됩니다.
     */
    void withdraw(Member member);

    /**
     * GitHub에서 레포지토리 목록을 가져와 DB에 저장합니다.
     * 이미 존재하는 레포는 description을 업데이트합니다.
     */
    MemberResDTO.GitRepoListDTO syncGitHubRepositories(Member member);
}
