package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.category.converter.CategoryConverter;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.exception.code.ImageErrorCode;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.converter.MemberConverter;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.ContactRepository;
import com.umc.devine.domain.techstack.converter.TechstackConverter;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.exception.TechstackException;
import com.umc.devine.domain.techstack.exception.code.TechstackErrorCode;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCommandServiceImpl implements MemberCommandService {

    private final MemberCategoryRepository memberCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final ContactRepository contactRepository;
    private final TechstackRepository techstackRepository;
    private final DevTechstackRepository devTechstackRepository;
    private final ImageRepository imageRepository;

    @Override
    public MemberResDTO.MemberProfileDTO updateMember(Member member, MemberReqDTO.UpdateMemberDTO dto) {

        // 프로필 이미지 검증
        if (dto.imageUrl() != null) {
            Image image = imageRepository.findByImageUrl(dto.imageUrl())
                    .orElseThrow(() -> new ImageException(ImageErrorCode.IMAGE_NOT_FOUND));
            if (image.getImageType() != ImageType.PROFILE) {
                throw new ImageException(ImageErrorCode.IMAGE_TYPE_MISMATCH);
            }
            if (!image.isUploaded()) {
                throw new ImageException(ImageErrorCode.IMAGE_NOT_UPLOADED);
            }
        }

        // 멤버 업데이트
        member.updateProfile(dto);

        // 카테고리 업데이트
        if (dto.domains() != null && dto.domains().length > 0) {
            memberCategoryRepository.deleteAllByMember(member);

            List<Category> categories = categoryRepository.findAllByGenreIn(Arrays.asList(dto.domains()));
            memberCategoryRepository.saveAll(CategoryConverter.toMemberCategories(member, categories));
        }

        // 연락처 업데이트
        if (dto.contacts() != null && dto.contacts().length > 0) {
            contactRepository.deleteAllByMember(member);
            contactRepository.saveAll(MemberConverter.toContacts(member, dto.contacts()));
        }

        return MemberConverter.toMemberProfileDTO(
                member,
                memberCategoryRepository.findAllByMember(member),
                contactRepository.findAllByMember(member)
        );
    }

    @Override
    public TechstackResDTO.DevTechstackListDTO addMemberTechstacks(Member member, MemberReqDTO.AddTechstackDTO dto) {
        List<Techstack> techstacks = techstackRepository.findAllById(Arrays.asList(dto.techstackIds()));

        if (techstacks.size() != dto.techstackIds().length) {
            throw new TechstackException(TechstackErrorCode.NOT_FOUND);
        }

        List<DevTechstack> existingTechstacks = devTechstackRepository.findAllByMemberAndTechstackIn(member, techstacks);
        if (!existingTechstacks.isEmpty()) {
            throw new MemberException(MemberErrorCode.TECHSTACK_ALREADY_EXISTS);
        }

        devTechstackRepository.saveAll(
                TechstackConverter.toDevTechstacks(member, techstacks, TechstackSource.MANUAL)
        );

        return TechstackConverter.toDevTechstackListDTO(devTechstackRepository.findAllByMember(member));
    }

    @Override
    public TechstackResDTO.DevTechstackListDTO removeMemberTechstacks(Member member, MemberReqDTO.RemoveTechstackDTO dto) {
        List<Techstack> techstacks = techstackRepository.findAllById(Arrays.asList(dto.techstackIds()));

        if (techstacks.size() != dto.techstackIds().length) {
            throw new TechstackException(TechstackErrorCode.NOT_FOUND);
        }

        List<DevTechstack> targetTechstacks = devTechstackRepository.findAllByMemberAndTechstackIn(member, techstacks);

        List<DevTechstack> deletedTechstacks = targetTechstacks.stream()
                .filter(dt -> dto.source() == null || dt.getSource() == dto.source())
                .toList();

        devTechstackRepository.deleteAll(deletedTechstacks);

        return TechstackConverter.toDevTechstackListDTO(deletedTechstacks);
    }
}
