package com.umc.devine.admin.complaint.service.query;

import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.global.dto.PagedResponse;

public interface ComplaintQueryService {

    PagedResponse<ComplaintResDTO.ComplaintSummaryDTO> getComplaintList(ComplaintReqDTO.SearchReq request);

    ComplaintResDTO.ComplaintDetailRes getComplaintDetail(Long complaintId);

    ComplaintResDTO.RespondentHistoryRes getRespondentHistory(Long memberId);
}
