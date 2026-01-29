package com.umc.devine.domain.image.service.command;

import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;

public interface ImageCommandService {

    ImageResDTO.PresignedUrlRes createPresignedUrl(Long memberId, ImageReqDTO.PresignedUrlReq request);

    void confirmUpload(Long imageId);

    void deleteImage(Long imageId);
}
