package com.umc.devine.admin.member.service.command;

import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface AdminMemberCommandService {

    AdminMemberResDTO.ChangeStatusRes changeStatus(String nickname, String processorClerkId, AdminMemberReqDTO.ChangeStatusReq request);

    /** 호출자가 이미 처리자(Member)를 조회해둔 경우, clerkId로 다시 조회하지 않고 그대로 전달하기 위한 오버로드. */
    AdminMemberResDTO.ChangeStatusRes changeStatus(String nickname, Member processor, AdminMemberReqDTO.ChangeStatusReq request);
}
