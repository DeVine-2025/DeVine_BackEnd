package com.umc.devine.domain.image.converter;

import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.member.entity.Member;

public class ImageConverter {

    public static Image toImage(ImageType imageType, String imageUrl, String s3Key, Member uploader) {
        return Image.builder()
                .imageType(imageType)
                .imageUrl(imageUrl)
                .s3Key(s3Key)
                .uploader(uploader)
                .build();
    }

    public static ImageResDTO.PresignedUrlRes toPresignedUrlRes(Image image, String presignedUrl) {
        return ImageResDTO.PresignedUrlRes.builder()
                .imageId(image.getId())
                .presignedUrl(presignedUrl)
                .imageUrl(image.getImageUrl())
                .build();
    }
}
