package com.umc.devine.domain.image.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageType {

    PROFILE("프로필 이미지"),
    PROJECT("프로젝트 대표사진"),
    EDITOR("에디터 본문 이미지");

    private final String displayName;
}
