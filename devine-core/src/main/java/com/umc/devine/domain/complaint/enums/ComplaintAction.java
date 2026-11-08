package com.umc.devine.domain.complaint.enums;

public enum ComplaintAction {
    WARNING,
    DELETE,
    SUSPEND,
    DISMISS,
    WITHDRAWN;

    public boolean isContentOnly() {
        return this == DELETE;
    }

    public boolean keepsBlinded() {
        return this == DELETE || this == SUSPEND;
    }
}
