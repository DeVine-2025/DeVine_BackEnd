package com.umc.devine.domain.complaint.service.command;

import com.umc.devine.domain.complaint.entity.Complaint;

public interface ComplaintModerationService {

    void blindOnCreate(Complaint complaint);

    void applyResolution(Complaint complaint);

    boolean isOwnerEditLocked(Long projectId);

    boolean hasConfirmedViolation(Long projectId);
}
