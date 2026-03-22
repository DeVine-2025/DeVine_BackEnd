package com.umc.devine.domain.image.service.command;

import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.infrastructure.s3.S3Service;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ImageCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private ImageCommandService imageCommandService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private S3Service s3Service;

    private Member testMember;
    private ClerkPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_image_test")
                .name("이미지테스트")
                .nickname("imagetest")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        testPrincipal = new ClerkPrincipal(
                "clerk_image_test", "image@test.com", "이미지테스트", null);
    }

    @Nested
    @DisplayName("Presigned URL 생성")
    class CreatePresignedUrlTest {

        @Test
        @DisplayName("프로젝트 이미지 Presigned URL 생성에 성공한다")
        void createPresignedUrl_project_success() throws Exception {
            // given
            ImageReqDTO.PresignedUrlReq request = ImageReqDTO.PresignedUrlReq.builder()
                    .imageType(ImageType.PROJECT)
                    .fileName("test.jpg")
                    .build();

            willDoNothing().given(s3Service).validateExtension("test.jpg");
            given(s3Service.buildProjectKey("test.jpg"))
                    .willReturn("projects/2026/01/01/uuid.jpg");
            given(s3Service.buildImageUrl("projects/2026/01/01/uuid.jpg"))
                    .willReturn("https://cdn.test.com/projects/2026/01/01/uuid.jpg");
            given(s3Service.guessContentType("test.jpg"))
                    .willReturn("image/jpeg");

            PresignedPutObjectRequest mockPresigned = mock(PresignedPutObjectRequest.class);
            given(mockPresigned.url()).willReturn(new URL("https://s3.amazonaws.com/presigned-url"));
            given(mockPresigned.expiration()).willReturn(Instant.now().plusSeconds(600));
            given(s3Service.generatePresignedPutUrl(anyString(), anyString()))
                    .willReturn(mockPresigned);

            // when
            ImageResDTO.PresignedUrlRes result = imageCommandService.createPresignedUrl(testPrincipal, request);

            // then
            assertThat(result.imageId()).isNotNull();
            assertThat(result.imageUrl()).isEqualTo("https://cdn.test.com/projects/2026/01/01/uuid.jpg");
            assertThat(result.presignedUrl()).contains("s3.amazonaws.com");

            // DB에 uploaded=false로 저장되었는지 확인
            Image saved = imageRepository.findById(result.imageId()).orElseThrow();
            assertThat(saved.isUploaded()).isFalse();
            assertThat(saved.getImageType()).isEqualTo(ImageType.PROJECT);
        }

        @Test
        @DisplayName("프로필 이미지 Presigned URL 생성에 성공한다")
        void createPresignedUrl_profile_success() throws Exception {
            // given
            ImageReqDTO.PresignedUrlReq request = ImageReqDTO.PresignedUrlReq.builder()
                    .imageType(ImageType.PROFILE)
                    .fileName("profile.png")
                    .build();

            willDoNothing().given(s3Service).validateExtension("profile.png");
            given(s3Service.buildProfileKey(anyString(), anyString()))
                    .willReturn("profiles/1/uuid.png");
            given(s3Service.buildImageUrl("profiles/1/uuid.png"))
                    .willReturn("https://cdn.test.com/profiles/1/uuid.png");
            given(s3Service.guessContentType("profile.png"))
                    .willReturn("image/png");

            PresignedPutObjectRequest mockPresigned = mock(PresignedPutObjectRequest.class);
            given(mockPresigned.url()).willReturn(new URL("https://s3.amazonaws.com/presigned-url"));
            given(mockPresigned.expiration()).willReturn(Instant.now().plusSeconds(600));
            given(s3Service.generatePresignedPutUrl(anyString(), anyString()))
                    .willReturn(mockPresigned);

            // when
            ImageResDTO.PresignedUrlRes result = imageCommandService.createPresignedUrl(testPrincipal, request);

            // then
            assertThat(result.imageId()).isNotNull();
            assertThat(result.imageUrl()).contains("profiles");
        }

        @Test
        @DisplayName("지원하지 않는 확장자이면 예외가 발생한다")
        void createPresignedUrl_unsupportedExtension() {
            // given
            ImageReqDTO.PresignedUrlReq request = ImageReqDTO.PresignedUrlReq.builder()
                    .imageType(ImageType.PROJECT)
                    .fileName("test.bmp")
                    .build();

            org.mockito.BDDMockito.willThrow(new ImageException(com.umc.devine.domain.image.exception.code.ImageErrorCode.UNSUPPORTED_FILE_EXTENSION))
                    .given(s3Service).validateExtension("test.bmp");

            // when & then
            assertThatThrownBy(() -> imageCommandService.createPresignedUrl(testPrincipal, request))
                    .isInstanceOf(ImageException.class);
        }
    }

    @Nested
    @DisplayName("업로드 확인")
    class ConfirmUploadTest {

        @Test
        @DisplayName("업로드 확인에 성공한다")
        void confirmUpload_success() {
            // given
            Image image = imageRepository.save(Image.builder()
                    .imageType(ImageType.PROJECT)
                    .imageUrl("https://cdn.test.com/test.jpg")
                    .s3Key("projects/test.jpg")
                    .uploaded(false)
                    .clerkId("clerk_image_test")
                    .uploader(testMember)
                    .build());

            // when
            imageCommandService.confirmUpload(testPrincipal, image.getId());

            // then
            Image confirmed = imageRepository.findById(image.getId()).orElseThrow();
            assertThat(confirmed.isUploaded()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 이미지 확인 시 예외가 발생한다")
        void confirmUpload_imageNotFound() {
            // when & then
            assertThatThrownBy(() -> imageCommandService.confirmUpload(testPrincipal, 999999L))
                    .isInstanceOf(ImageException.class);
        }

        @Test
        @DisplayName("다른 사용자의 이미지 확인 시 예외가 발생한다")
        void confirmUpload_accessDenied() {
            // given
            Image image = imageRepository.save(Image.builder()
                    .imageType(ImageType.PROJECT)
                    .imageUrl("https://cdn.test.com/other.jpg")
                    .s3Key("projects/other.jpg")
                    .uploaded(false)
                    .clerkId("clerk_other_user")
                    .uploader(null)
                    .build());

            // when & then
            assertThatThrownBy(() -> imageCommandService.confirmUpload(testPrincipal, image.getId()))
                    .isInstanceOf(ImageException.class);
        }
    }

    @Nested
    @DisplayName("이미지 삭제")
    class DeleteImageTest {

        @Test
        @DisplayName("이미지 삭제에 성공한다")
        void deleteImage_success() {
            // given
            Image image = imageRepository.save(Image.builder()
                    .imageType(ImageType.PROJECT)
                    .imageUrl("https://cdn.test.com/delete.jpg")
                    .s3Key("projects/delete.jpg")
                    .uploaded(true)
                    .clerkId("clerk_image_test")
                    .uploader(testMember)
                    .build());

            willDoNothing().given(s3Service).deleteObject("projects/delete.jpg");

            // when
            imageCommandService.deleteImage(testPrincipal, image.getId());

            // then
            Optional<Image> deleted = imageRepository.findById(image.getId());
            assertThat(deleted).isEmpty();
            verify(s3Service).deleteObject("projects/delete.jpg");
        }

        @Test
        @DisplayName("존재하지 않는 이미지 삭제 시 예외가 발생한다")
        void deleteImage_imageNotFound() {
            // when & then
            assertThatThrownBy(() -> imageCommandService.deleteImage(testPrincipal, 999999L))
                    .isInstanceOf(ImageException.class);
        }

        @Test
        @DisplayName("다른 사용자의 이미지 삭제 시 예외가 발생한다")
        void deleteImage_accessDenied() {
            // given
            Image image = imageRepository.save(Image.builder()
                    .imageType(ImageType.PROJECT)
                    .imageUrl("https://cdn.test.com/other.jpg")
                    .s3Key("projects/other.jpg")
                    .uploaded(true)
                    .clerkId("clerk_other_user")
                    .uploader(null)
                    .build());

            // when & then
            assertThatThrownBy(() -> imageCommandService.deleteImage(testPrincipal, image.getId()))
                    .isInstanceOf(ImageException.class);
        }

        @Test
        @DisplayName("clerkId가 아닌 uploader 기반으로도 소유권 검증이 통과한다")
        void deleteImage_uploaderFallback() {
            // given - clerkId는 다르지만 uploader의 clerkId가 일치
            Image image = imageRepository.save(Image.builder()
                    .imageType(ImageType.PROJECT)
                    .imageUrl("https://cdn.test.com/fallback.jpg")
                    .s3Key("projects/fallback.jpg")
                    .uploaded(true)
                    .clerkId("old_clerk_id")
                    .uploader(testMember)
                    .build());

            willDoNothing().given(s3Service).deleteObject("projects/fallback.jpg");

            // when
            imageCommandService.deleteImage(testPrincipal, image.getId());

            // then
            Optional<Image> deleted = imageRepository.findById(image.getId());
            assertThat(deleted).isEmpty();
        }
    }
}
