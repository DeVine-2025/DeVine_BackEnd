package com.umc.devine.domain.complaint.service.command;

import com.umc.devine.domain.complaint.dto.ComplaintReqDTO;
import com.umc.devine.domain.complaint.dto.ComplaintResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface ComplaintCommandService {

    ComplaintResDTO.CreateRes create(Member complainant, ComplaintReqDTO.CreateReq request);
}
