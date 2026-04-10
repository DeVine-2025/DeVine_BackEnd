package com.umc.devine.domain.techstack.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DevTechstackCommandServiceImpl implements DevTechstackCommandService {

    private final TechstackRepository techstackRepository;
    private final DevTechstackRepository devTechstackRepository;

    @Override
    public void saveAutoTechstacks(Member member, List<String> techstackNames) {
        if (techstackNames == null || techstackNames.isEmpty()) {
            log.info("저장할 techstacks가 없습니다. memberId: {}", member.getId());
            return;
        }

        List<TechName> techNames = techstackNames.stream()
                .map(name -> {
                    try {
                        return TechName.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        log.warn("알 수 없는 TechName: {}", name);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        if (techNames.isEmpty()) {
            log.info("유효한 techstacks가 없습니다. memberId: {}", member.getId());
            return;
        }

        List<Techstack> techstacks = techstackRepository.findAllByNameInWithParent(techNames);

        if (techstacks.isEmpty()) {
            log.info("매칭되는 Techstack이 없습니다. memberId: {}", member.getId());
            return;
        }

        // 하위 techstack + parent를 모두 수집 (중복 제거)
        Set<Techstack> allTechstacks = new HashSet<>(techstacks);
        for (Techstack ts : techstacks) {
            if (ts.getParentStack() != null) {
                allTechstacks.add(ts.getParentStack());
            }
        }

        List<Techstack> techstackList = new ArrayList<>(allTechstacks);

        List<DevTechstack> existingDevTechstacks = devTechstackRepository.findAllByMemberAndTechstackInWithTechstack(member, techstackList);
        Map<Long, DevTechstack> existingMap = existingDevTechstacks.stream()
                .collect(Collectors.toMap(dt -> dt.getTechstack().getId(), dt -> dt));

        List<DevTechstack> toSave = new ArrayList<>();
        int updatedCount = 0;

        for (Techstack ts : techstackList) {
            DevTechstack existing = existingMap.get(ts.getId());

            if (existing == null) {
                toSave.add(DevTechstack.builder()
                        .member(member)
                        .techstack(ts)
                        .source(TechstackSource.AUTO)
                        .build());
            } else if (existing.getSource() == TechstackSource.MANUAL) {
                // MANUAL → AUTO로 업데이트 (AUTO가 더 강한 권한)
                existing.updateSourceToAuto();
                updatedCount++;
            }
        }

        if (!toSave.isEmpty()) {
            devTechstackRepository.saveAll(toSave);
            log.info("DevTechstack AUTO 신규 저장 - memberId: {}, count: {}", member.getId(), toSave.size());
        }

        if (updatedCount > 0) {
            log.info("DevTechstack MANUAL → AUTO 업데이트 - memberId: {}, count: {}", member.getId(), updatedCount);
        }
    }
}