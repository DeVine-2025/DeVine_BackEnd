package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;

public interface MatchingCommandService {

    MatchingResDTO.ProposeResDTO applyToProject(Member member, Long projectId);

    MatchingResDTO.ProposeResDTO cancelApplication(Member member, Long projectId);

    MatchingResDTO.ProposeResDTO proposeToMember(Member member, String targetNickname, Long projectId);
}
