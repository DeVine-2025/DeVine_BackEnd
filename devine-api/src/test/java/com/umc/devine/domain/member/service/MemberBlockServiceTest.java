package com.umc.devine.domain.member.service;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.MemberBlockRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.*;

class MemberBlockServiceTest extends IntegrationTestSupport {
    @Autowired MemberBlockService service;
    @Autowired MemberRepository members;
    @Autowired MemberBlockRepository blocks;

    private Member member(String nickname) {
        return members.save(Member.builder().clerkId(nickname).name(nickname).nickname(nickname)
                .mainType(MemberMainType.DEVELOPER).used(MemberStatus.ACTIVE).build());
    }

    @Test
    void blockAndUnblockAreIdempotentAndListIsOutgoingOnly() {
        Member a = member("servicea");
        Member b = member("serviceb");
        service.block(a, b.getNickname());
        service.block(a, b.getNickname());
        assertThat(service.list(a, PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(service.list(b, PageRequest.of(0, 10)).getContent()).isEmpty();
        service.block(b, a.getNickname());
        service.unblock(a, b.getNickname());
        service.unblock(a, b.getNickname());
        assertThat(blocks.existsBetween(a.getId(), b.getId())).isTrue();
        service.unblock(b, a.getNickname());
        assertThat(blocks.existsBetween(a.getId(), b.getId())).isFalse();
    }

    @Test
    void rejectsSelfAndWithdrawnTargetButCanUnblockWithdrawnTarget() {
        Member a = member("selfa");
        Member b = member("selfb");
        assertThatThrownBy(() -> service.block(a, a.getNickname())).isInstanceOf(MemberException.class);
        service.block(a, b.getNickname());
        b.withdraw();
        assertThatThrownBy(() -> service.block(a, b.getNickname())).isInstanceOf(MemberException.class);
        service.unblock(a, b.getNickname());
        assertThat(blocks.existsBetween(a.getId(), b.getId())).isFalse();
    }
}
