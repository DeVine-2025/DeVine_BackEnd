package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.MemberBlock;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class MemberBlockRepositoryTest extends CoreIntegrationTestSupport {
    @Autowired MemberRepository members;
    @Autowired MemberBlockRepository blocks;

    private Member member(String nickname) {
        return members.save(Member.builder().clerkId(nickname).name(nickname).nickname(nickname)
                .mainType(MemberMainType.DEVELOPER).disclosure(true)
                .used(com.umc.devine.domain.member.enums.MemberStatus.ACTIVE).build());
    }

    @Test
    void eitherDirectionAppliesAndUnblockingOneSideKeepsTheOther() {
        Member a = member("blocka");
        Member b = member("blockb");
        blocks.save(MemberBlock.builder().blocker(a).target(b).build());
        blocks.save(MemberBlock.builder().blocker(b).target(a).build());
        assertThat(blocks.existsBetween(a.getId(), b.getId())).isTrue();
        assertThat(blocks.findExcludedMemberIds(a.getId())).containsExactly(b.getId());
        blocks.deleteByBlockerIdAndTargetId(a.getId(), b.getId());
        assertThat(blocks.existsBetween(a.getId(), b.getId())).isTrue();
        blocks.deleteByBlockerIdAndTargetId(b.getId(), a.getId());
        assertThat(blocks.existsBetween(a.getId(), b.getId())).isFalse();
    }
}
