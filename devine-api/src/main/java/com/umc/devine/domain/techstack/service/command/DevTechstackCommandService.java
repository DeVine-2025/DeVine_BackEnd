package com.umc.devine.domain.techstack.service.command;

import com.umc.devine.domain.member.entity.Member;

import java.util.List;

public interface DevTechstackCommandService {
    void saveAutoTechstacks(Member member, List<String> techstackNames);
}