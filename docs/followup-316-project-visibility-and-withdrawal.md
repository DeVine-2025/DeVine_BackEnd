# 프로젝트 노출 기능 병합 후 후속 작업

이 문서는 관리자 TODO 해결과 회원 자진 탈퇴 작업 완료 시점 기준으로, 아직 병합되지 않은 프로젝트 노출/비노출 기능 브랜치가 나중에 병합되면 반드시 해야 하는 후속 작업을 정리한 것입니다.

## 목차

1. [신고 처리와 프로젝트 비노출 연동 재정리](#1-신고-처리와-프로젝트-비노출-연동-재정리)
2. [회원 탈퇴 시 소유 프로젝트 처리 설계](#2-회원-탈퇴-시-소유-프로젝트-처리-설계)

---

## 1. 신고 처리와 프로젝트 비노출 연동 재정리

### 지금 상태

`ComplaintCommandServiceImpl.updateStatus`의 현재 구조는 다음과 같습니다.

```java
public ComplaintResDTO.UpdateStatusRes updateStatus(Long complaintId, String processorClerkId, ComplaintReqDTO.UpdateStatusReq request) {
    ...
    Member resolver = processorClerkId != null ? memberRepository.findByClerkId(processorClerkId).orElse(null) : null;

    if (request.status() == ComplaintStatus.COMPLETED) {
        if (action == ComplaintAction.SUSPEND) {
            suspendRespondent(complaint, resolver, resolutionReason);
        }
        if (action == ComplaintAction.DELETE && complaint.getTargetType() == ComplaintTargetType.PROJECT) {
            hideReportedProject(complaint.getTargetId());
        }
    }

    complaint.updateStatus(request.status(), action, resolutionReason, resolver, resolvedAt);
    ...
}

private void hideReportedProject(Long projectId) {
    projectRepository.findById(projectId)
            .filter(project -> !ProjectStatus.INVISIBLE_STATUSES.contains(project.getStatus()))
            .ifPresent(Project::hide);
}

private void suspendRespondent(Complaint complaint, Member resolver, String resolutionReason) {
    Member respondent = complaint.getRespondentMember();
    if (respondent.getUsed() != MemberStatus.ACTIVE && respondent.getUsed() != MemberStatus.INACTIVE) {
        return; // 이미 정지나 탈퇴 등 최종 상태면 멱등 처리
    }
    adminMemberCommandService.changeStatus(respondent.getNickname(), resolver, ...);
}
```

**핵심 포인트**

- `updateStatus(Long complaintId, String processorClerkId, ...)`. 파라미터 타입은 여전히 `String`(clerkId 기준)입니다.
- `resolver`(`Member`)는 메서드 최상단에서 한 번만 조회하고, `suspendRespondent()`와 `complaint.updateStatus()`, `ComplaintHistory` 양쪽에 그대로 재사용합니다. 이전엔 두 번 조회했는데, 이번에 중복 조회 문제를 고쳤습니다.
- `AdminMemberCommandService`에 두 개의 오버로드가 있습니다.
  - 기존 `changeStatus(String nickname, String processorClerkId, req)`. 컨트롤러용이며 clerkId로 내부에서 조회합니다.
  - 신규 `changeStatus(String nickname, Member processor, req)`. 이미 조회해둔 Member를 그대로 전달하며, 이 신고 처리 경로가 사용합니다.
- `suspendRespondent()`에 멱등 가드가 있습니다. 피신고자가 `ACTIVE`나 `INACTIVE`가 아니면(이미 `SUSPENDED`, `PENDING_WITHDRAWAL`, `DELETED`) 조용히 스킵합니다. 이 가드가 없으면 상습 위반자를 두 번째 신고에서 SUSPEND 처리할 때 회원 도메인 예외가 신고 트랜잭션 전체를 롤백시킵니다.
- 프로젝트 비노출은 여전히 `ProjectStatus.HIDDEN` 상태값을 덮어쓰는 옛날 방식(`Project.hide()`)입니다.

### 프로젝트 노출 기능이 도입하는 것

프로젝트 노출/비노출 기능 브랜치는 같은 `ComplaintCommandServiceImpl.updateStatus`를 다음과 같이 바꿔놓았습니다.

- `hideReportedProject()`를 제거하고, `ProjectVisibilityCommandService.hideForModeration(projectId)` 호출로 대체합니다.
  - `is_hidden` 플래그 방식으로 동작하며, `ProjectStatus.HIDDEN`은 완전히 제거됩니다.
  - 신고 상태변경과 같은 트랜잭션에서 동작해야 하므로, 대상이 없거나 이미 삭제된 경우에도 예외를 던지지 않고 조용히 넘어가도록 구현되어 있습니다. 트랜잭션 전체 롤백을 막기 위한 것이니 이 특성은 그대로 유지해야 합니다.
  - 처리 완료 시 `Complaint.linked_action_completed`와 `linked_action_at`을 기록해, 신고 상세와 상태변경 응답에 `linkedActionCompleted`로 노출합니다.
- `updateStatus`의 파라미터 타입을 `String processorClerkId`에서 `Long processorMemberId`로 바꿔놓았습니다. 이 브랜치가 갈라져 나온 시점에는 관리자 인증이 아직 확정 전이었기 때문입니다.

### 병합 시 체크리스트

- [ ] `updateStatus`의 파라미터 타입은 dev 기준(`String processorClerkId`)으로 유지합니다. 프로젝트 노출 기능이 바꿔놓은 `Long processorMemberId`는 되돌립니다.
- [ ] `hideReportedProject(Long projectId)` 메서드를 제거하고, 그 호출부를 `ProjectVisibilityCommandService.hideForModeration(projectId)` 호출로 교체합니다. 더 이상 쓰이지 않는 `ProjectRepository`와 `ProjectStatus` import는 정리합니다.
- [ ] `suspendRespondent()`(SUSPEND 연동과 멱등 가드)는 그대로 유지합니다. 프로젝트 노출 기능은 이 로직을 모른 채 갈라진 브랜치라 충돌 마커에만 나타날 뿐, 실제로 없앨 이유는 없습니다.
- [ ] `resolver`를 메서드 최상단에서 한 번만 조회하고 재사용하는 구조, 그리고 `AdminMemberCommandService`의 `Member` 기반 오버로드도 그대로 유지합니다.
- [ ] `ComplaintCommandServiceTest.java`에서, 프로젝트 노출 기능이 추가한 프로젝트 비노출 관련 테스트(멱등성, 대상 없음 케이스 등)와 이번에 추가한 SUSPEND 연동 및 멱등성 테스트를 모두 남기고 합칩니다. `updateStatus` 호출부의 파라미터 타입을 전부 `String` 기준으로 맞춥니다.
- [ ] `ApiSecurityConfig.java`는 프로젝트 노출 기능이 건드리지 않은 파일이지만, 오래된 브랜치라 dev의 관리자 인증 permitAll 정리 이전 상태와 충돌합니다. 이는 이 병합과 무관한 충돌이므로 dev의 최신 상태(permitAll이 제거된 상태)를 그대로 채택합니다.
- [ ] 병합 후 전체 테스트를 재실행해 회귀를 확인합니다.

---

## 2. 회원 탈퇴 시 소유 프로젝트 처리 설계

### 지금 상태

`MemberWithdrawalCommandServiceImpl.selfWithdraw()`, `MemberWithdrawalFinalizeScheduler`(강제탈퇴 확정), `MemberHardDeleteScheduler`와 그 핸들러 구현체들 어디에도 `Project` 관련 처리가 없습니다.

- 탈퇴 회원이 PM으로 등록한 프로젝트는 그대로 남아있습니다. 상태 변경도, 비노출 처리도 하지 않습니다.
- 탈퇴 회원의 닉네임과 이름 등 PII는 즉시 익명화되므로, 프로젝트 작성자 표시가 익명화된 값으로 바뀌는 효과는 있지만 이는 부수 효과일 뿐 의도적으로 설계한 동작이 아닙니다.
- 프로젝트에 걸린 매칭(지원, 제안, 수락) 이력과 채팅방 등도 그대로 남아있습니다.
- Hard Delete 배치는 payment, matching, project, chat 등 다른 회원과 얽힌 레코드를 의도적으로 건드리지 않고, 이런 레코드가 남아있으면 FK 위반으로 해당 회원의 하드삭제 자체를 건너뜁니다. 즉 소유 프로젝트가 있는 PM은 하드삭제 자체가 계속 스킵될 수 있다는 뜻이기도 합니다.

이렇게 된 이유는, 프로젝트가 삭제 요청인데 실제로는 비활성화되는 느낌으로 처리되는 기존 동작이 프로젝트 노출 기능 브랜치(미병합)에서 `is_hidden` 플래그 방식으로 재정비되고 있었기 때문입니다. 그 브랜치가 병합되기 전에 프로젝트 처리 로직을 새로 짜면 이중 작업이 될 위험이 있어, 이번 스코프에서 완전히 제외했습니다.

### 병합 후 결정해야 할 것

- **탈퇴 시 소유 프로젝트를 자동으로 비노출 처리할 것인가**
  - 한다면 `ProjectVisibilityCommandService`의 어떤 메서드를 재사용할지 정해야 합니다. 관리자용 `changeVisibility()`는 처리자가 필요하므로, 시스템에 의한 자동 처리용 별도 경로가 필요할 수 있습니다.
  - `visibility_changed_by`에 탈퇴 처리 주체(시스템 또는 스케줄러)를 어떻게 기록할지도 정해야 합니다.
- **모집 중인 프로젝트에 지원자가 있는 상태에서 PM이 탈퇴하면**
  - 지원자에게 알림이 필요한지, 프로젝트를 자동으로 비노출하는 것이 지원자 경험에 문제가 없는지 검토가 필요합니다.
- **진행 중인 프로젝트의 PM이 탈퇴하면**
  - 함께 참여 중인 개발자가 있는 프로젝트를 비노출하는 게 맞는지, 아니면 다른 처리(소유권 이전 등, 이번 문서 범위 밖)가 필요한지 검토가 필요합니다.
- **자진 탈퇴와 강제 탈퇴에서 처리가 달라야 하는가**
  - 강제 탈퇴는 대개 제재 성격이 강하므로 즉시 비노출이 타당할 수 있지만, 자진 탈퇴는 단순 개인 사정일 수 있어 다른 정책이 필요할 수 있습니다.
- **Hard Delete 배치 시점에는 어떻게 할 것인가**
  - `Project.member` FK가 남아있는 한 하드삭제 자체가 계속 스킵되는 현재 동작을 그대로 둘지, 아니면 프로젝트 소유권을 시스템 계정으로 이전하는 등의 방법으로 하드삭제를 진행할 수 있게 할지 재검토가 필요합니다.

### 관련 코드 위치

- `MemberWithdrawalCommandServiceImpl.java`: 자진 탈퇴 오케스트레이션
- `MemberWithdrawalFinalizeScheduler.java`: 강제탈퇴 확정 스케줄러
- `MemberHardDeleteScheduler.java`와 `global/scheduler/harddelete` 패키지: 하드삭제 배치(핸들러 전략 패턴). 프로젝트 처리가 필요해지면 `MemberHardDeleteHandler` 구현체를 새로 추가하면 됩니다.
