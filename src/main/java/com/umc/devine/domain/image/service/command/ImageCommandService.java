package com.umc.devine.domain.image.service.command;

import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface ImageCommandService {

    ImageResDTO.PresignedUrlRes createPresignedUrl(Member member, ImageReqDTO.PresignedUrlReq request);

    void confirmUpload(Member member, Long imageId);

    void deleteImage(Member member, Long imageId);
}
