package com.umc.devine.domain.image.repository;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectImage;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.repository.ProjectImageRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ImageRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectImageRepository projectImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_img_repo")
                .name("이미지레포")
                .nickname("imgrepo")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    private Image createImage(ImageType type, boolean uploaded, String clerkId) {
        return imageRepository.save(Image.builder()
                .imageType(type)
                .imageUrl("https://cdn.test.com/" + type.name().toLowerCase() + "/" + System.nanoTime() + ".jpg")
                .s3Key(type.name().toLowerCase() + "/" + System.nanoTime() + ".jpg")
                .uploaded(uploaded)
                .clerkId(clerkId)
                .uploader(testMember)
                .build());
    }

    @Nested
    @DisplayName("findByImageUrl")
    class FindByImageUrlTest {

        @Test
        @DisplayName("이미지 URL로 조회에 성공한다")
        void findByImageUrl_success() {
            // given
            Image image = imageRepository.save(Image.builder()
                    .imageType(ImageType.PROJECT)
                    .imageUrl("https://cdn.test.com/unique-url.jpg")
                    .s3Key("projects/unique-url.jpg")
                    .uploaded(true)
                    .clerkId("clerk_img_repo")
                    .uploader(testMember)
                    .build());

            // when
            Optional<Image> result = imageRepository.findByImageUrl("https://cdn.test.com/unique-url.jpg");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(image.getId());
        }

        @Test
        @DisplayName("존재하지 않는 URL로 조회 시 빈 결과를 반환한다")
        void findByImageUrl_notFound() {
            // when
            Optional<Image> result = imageRepository.findByImageUrl("https://cdn.test.com/nonexistent.jpg");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findUnconfirmedImages")
    class FindUnconfirmedImagesTest {

        @Test
        @DisplayName("확인되지 않은 오래된 이미지를 조회한다")
        void findUnconfirmedImages_success() {
            // given
            createImage(ImageType.PROJECT, false, "clerk_img_repo");
            createImage(ImageType.PROJECT, true, "clerk_img_repo");

            // threshold를 미래로 설정하면 모든 미확인 이미지가 조회됨
            LocalDateTime futureThreshold = LocalDateTime.now().plusDays(1);

            // when
            List<Image> result = imageRepository.findUnconfirmedImages(futureThreshold);

            // then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(img -> !img.isUploaded());
        }

        @Test
        @DisplayName("모든 이미지가 확인된 경우 빈 결과를 반환한다")
        void findUnconfirmedImages_allConfirmed() {
            // given
            createImage(ImageType.PROJECT, true, "clerk_img_repo");

            // 과거 threshold - 방금 생성된 이미지는 해당하지 않음
            LocalDateTime pastThreshold = LocalDateTime.now().minusDays(1);

            // when
            List<Image> result = imageRepository.findUnconfirmedImages(pastThreshold);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOrphanProjectImages")
    class FindOrphanProjectImagesTest {

        @Test
        @DisplayName("프로젝트에 연결되지 않은 고아 이미지를 조회한다")
        void findOrphanProjectImages_success() {
            // given
            Category category = categoryRepository.findByGenre(CategoryGenre.FINTECH).orElseThrow();

            // 프로젝트에 연결된 이미지
            Image linkedImage = createImage(ImageType.PROJECT, true, "clerk_img_repo");
            Project project = projectRepository.save(Project.builder()
                    .name("연결 프로젝트")
                    .content("내용")
                    .status(ProjectStatus.RECRUITING)
                    .projectField(ProjectField.WEB)
                    .mode(ProjectMode.ONLINE)
                    .durationRange(DurationRange.ONE_TO_THREE)
                    .location("온라인")
                    .recruitmentDeadline(LocalDate.now().plusDays(30))
                    .category(category)
                    .member(testMember)
                    .build());
            projectImageRepository.save(ProjectImage.builder()
                    .project(project)
                    .image(linkedImage)
                    .build());

            // 고아 이미지 (프로젝트에 연결되지 않음)
            Image orphanImage = createImage(ImageType.PROJECT, true, "clerk_img_repo");

            LocalDateTime futureThreshold = LocalDateTime.now().plusDays(1);

            // when
            List<Image> result = imageRepository.findOrphanProjectImages(futureThreshold);

            // then
            assertThat(result).contains(orphanImage);
            assertThat(result).doesNotContain(linkedImage);
        }
    }
}
