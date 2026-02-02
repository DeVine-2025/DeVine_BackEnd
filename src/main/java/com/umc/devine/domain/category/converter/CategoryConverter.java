package com.umc.devine.domain.category.converter;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.entity.mapping.MemberCategory;
import com.umc.devine.domain.member.entity.Member;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryConverter {

    public static MemberCategory toMemberCategory(Member member, Category category) {
        return MemberCategory.builder()
                .member(member)
                .category(category)
                .build();
    }

    public static List<MemberCategory> toMemberCategories(Member member, List<Category> categories) {
        return categories.stream()
                .map(category -> toMemberCategory(member, category))
                .collect(Collectors.toList());
    }
}
