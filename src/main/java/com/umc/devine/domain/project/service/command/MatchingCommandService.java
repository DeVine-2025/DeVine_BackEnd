package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.project.dto.matching.MatchingResDTO;

public interface MatchingCommandService {

    MatchingResDTO.ProposeResDTO applyToProject(Long memberId, Long projectId);

    MatchingResDTO.ProposeResDTO cancelApplication(Long memberId, Long projectId);

    MatchingResDTO.ProposeResDTO proposeToMember(Long pmMemberId, String targetNickname, Long projectId);
}
