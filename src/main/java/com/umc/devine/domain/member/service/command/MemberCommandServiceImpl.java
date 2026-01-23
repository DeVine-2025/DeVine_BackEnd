package com.umc.devine.domain.member.service.command;

import com.umc.devine.domain.category.converter.CategoryConverter;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.category.repository.MemberCategoryRepository;
import com.umc.devine.domain.member.converter.MemberConverter;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.ContactRepository;
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

    @Override
    public MemberResDTO.MemberProfileDTO updateMember(Member member, MemberReqDTO.UpdateMemberDTO dto) {
        
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
}
