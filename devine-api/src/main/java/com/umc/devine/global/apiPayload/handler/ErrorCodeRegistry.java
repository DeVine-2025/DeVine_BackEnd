package com.umc.devine.global.apiPayload.handler;

import com.umc.devine.domain.auth.exception.code.AuthErrorCode;
import com.umc.devine.domain.bookmark.exception.code.BookmarkErrorCode;
import com.umc.devine.domain.category.exception.code.CategoryErrorCode;
import com.umc.devine.domain.image.exception.code.ImageErrorCode;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.notification.exception.code.NotificationErrorCode;
import com.umc.devine.domain.project.exception.code.MatchingErrorCode;
import com.umc.devine.domain.project.exception.code.ProjectErrorCode;
import com.umc.devine.domain.report.exception.code.ReportErrorCode;
import com.umc.devine.domain.techstack.exception.code.TechstackErrorCode;
import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.apiPayload.code.GeneralErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ErrorCodeRegistry {

    private static final Map<DomainErrorReason, BaseErrorCode> REGISTRY = new HashMap<>();

    static {
        register(GeneralErrorCode.values());
        register(AuthErrorCode.values());
        register(MemberErrorCode.values());
        register(ProjectErrorCode.values());
        register(MatchingErrorCode.values());
        register(BookmarkErrorCode.values());
        register(CategoryErrorCode.values());
        register(ImageErrorCode.values());
        register(NotificationErrorCode.values());
        register(ReportErrorCode.values());
        register(TechstackErrorCode.values());
    }

    private static void register(BaseErrorCode[] codes) {
        for (BaseErrorCode code : codes) {
            REGISTRY.put(code.getReason(), code);
        }
    }

    public static Optional<BaseErrorCode> resolve(DomainErrorReason reason) {
        return Optional.ofNullable(REGISTRY.get(reason));
    }
}
