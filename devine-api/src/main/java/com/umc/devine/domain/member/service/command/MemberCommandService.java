package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.ClerkPrincipal;

public interface MemberCommandService {
    MemberResDTO.SignupResultDTO signup(ClerkPrincipal principal, MemberReqDTO.SignupDTO dto);
    MemberResDTO.MemberProfileDTO updateMember(Member member, MemberReqDTO.UpdateMemberDTO dto);
    TechstackResDTO.DevTechstackListDTO addMemberTechstacks(Member member, MemberReqDTO.AddTechstackDTO dto);
    TechstackResDTO.DevTechstackListDTO removeMemberTechstacks(Member member, MemberReqDTO.RemoveTechstackDTO dto);

    /**
     * 회원 탈퇴(논리 삭제)를 처리합니다.
     * 상태를 DELETED로 변경하고, 탈퇴 시각 기록 및 개인정보 익명화를 수행합니다.
     * 트랜잭션 커밋 후 이벤트 리스너가 Clerk 사용자 삭제를 비동기로 호출합니다.
     */
    void withdraw(Member member);

    /**
     * GitHub에서 레포지토리 목록을 가져와 DB에 저장하고, 페이지네이션된 결과를 반환합니다.
     * 이미 존재하는 레포는 description을 업데이트합니다.
     */
    PagedResponse<MemberResDTO.GitRepoDTO> syncGitHubRepositories(Member member, MemberReqDTO.GitRepoSyncDTO dto);
}
