package com.umc.devine.admin.complaint.service.command;

import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;

public interface ComplaintCommandService {

    ComplaintResDTO.UpdateStatusRes updateStatus(Long complaintId, String processorClerkId, ComplaintReqDTO.UpdateStatusReq request);
}
