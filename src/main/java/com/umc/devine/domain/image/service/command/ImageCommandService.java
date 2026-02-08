package com.umc.devine.domain.image.service.command;

import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.global.auth.ClerkPrincipal;

public interface ImageCommandService {

    ImageResDTO.PresignedUrlRes createPresignedUrl(ClerkPrincipal principal, ImageReqDTO.PresignedUrlReq request);

    void confirmUpload(ClerkPrincipal principal, Long imageId);

    void deleteImage(ClerkPrincipal principal, Long imageId);
}
