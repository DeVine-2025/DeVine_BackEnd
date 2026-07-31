package com.umc.devine.admin.member.service.query;

import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.global.dto.PagedResponse;

public interface AdminMemberQueryService {

    PagedResponse<AdminMemberResDTO.MemberSummaryDTO> getMemberList(AdminMemberReqDTO.SearchReq request);

    AdminMemberResDTO.MemberDetailRes getMemberDetail(String nickname);
}
