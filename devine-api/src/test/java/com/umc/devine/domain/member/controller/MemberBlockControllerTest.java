package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.*;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MemberBlockControllerTest extends ControllerIntegrationTestSupport {
    @Autowired MemberRepository members;
    Authentication auth;

    @BeforeEach
    void setup() {
        for (String nickname : List.of("blockviewer", "blocktarget")) {
            members.save(Member.builder().clerkId(nickname).name("회원").nickname(nickname)
                    .used(MemberStatus.ACTIVE).mainType(MemberMainType.DEVELOPER).build());
        }
        auth = new UsernamePasswordAuthenticationToken(new ClerkPrincipal("blockviewer", null, null, null), null, List.of());
    }

    @Test
    void requiresAuthenticationAndValidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/member-blocks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/member-blocks").with(authentication(auth)).param("page", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/member-blocks").with(authentication(auth)).param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blockLifecycleUsesNicknameAndHidesAuthenticatedPublicProfile() throws Exception {
        mockMvc.perform(get("/api/v1/member-blocks/blocktarget/confirmation").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.pendingCount").value(0));
        mockMvc.perform(post("/api/v1/member-blocks/blocktarget").with(authentication(auth))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/member-blocks/blocktarget").with(authentication(auth))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/member-blocks").with(authentication(auth)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.content[0].nickname").value("blocktarget"))
                .andExpect(jsonPath("$.result.content[0].memberId").doesNotExist())
                .andExpect(jsonPath("$.result.totalElements").value(1));
        mockMvc.perform(get("/api/v1/members/blocktarget").with(authentication(auth))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/members/blocktarget")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/member-blocks/blocktarget").with(authentication(auth))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/member-blocks/blocktarget").with(authentication(auth))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/members/blocktarget").with(authentication(auth))).andExpect(status().isOk());
    }
}
