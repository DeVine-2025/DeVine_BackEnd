package com.umc.devine.admin.member.service.command;

import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;

public interface AdminMemberCommandService {

    AdminMemberResDTO.ChangeStatusRes changeStatus(String nickname, Long processorMemberId, AdminMemberReqDTO.ChangeStatusReq request);
}
