package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 이미지 자산은 다른 곳에서 계속 참조될 수 있어 삭제하지 않고 업로더 참조만 끊는다. */
@Component
@Order(100)
@RequiredArgsConstructor
public class ImageHardDeleteHandler implements MemberHardDeleteHandler {

    private final ImageRepository imageRepository;

    @Override
    public void handle(Member member) {
        imageRepository.bulkNullifyUploader(member);
    }
}
