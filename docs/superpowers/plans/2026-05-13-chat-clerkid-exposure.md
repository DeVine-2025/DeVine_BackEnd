# Chat clerkId Exposure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 채팅방 생성에 필요한 `clerkId`를 개발자 상세 응답(`MemberProfileDTO`)과 프로젝트 상세 응답(`UpdateProjectRes`)에 노출한다.

**Architecture:** `MemberDetailDTO`는 목록 조회에도 재사용되므로 건드리지 않는다. `MemberProfileDTO`(단건 프로필 응답)에 `clerkId`를 직접 추가하고, `UpdateProjectRes`(프로젝트 상세)에 `creatorClerkId`를 추가한다. Converter에서 각각 값을 채운다.

**Tech Stack:** Java 21 record, Spring Boot, Lombok Builder

---

## File Map

| 파일 | 변경 내용 |
|---|---|
| `devine-api/src/main/java/com/umc/devine/domain/member/dto/MemberResDTO.java` | `MemberProfileDTO`에 `clerkId` 필드 추가 |
| `devine-api/src/main/java/com/umc/devine/domain/member/converter/MemberConverter.java` | `toMemberProfileDTO`에 `clerkId` 값 세팅 |
| `devine-api/src/main/java/com/umc/devine/domain/project/dto/ProjectResDTO.java` | `UpdateProjectRes`에 `creatorClerkId` 필드 추가 |
| `devine-api/src/main/java/com/umc/devine/domain/project/converter/ProjectConverter.java` | `toUpdateProjectRes`에 `creatorClerkId` 값 세팅 |

---

### Task 1: MemberProfileDTO에 clerkId 추가

**Files:**
- Modify: `devine-api/src/main/java/com/umc/devine/domain/member/dto/MemberResDTO.java:96-100`

- [ ] **Step 1: MemberProfileDTO record에 clerkId 필드 추가**

```java
@Builder
public record MemberProfileDTO(
        MemberDetailDTO member,
        List<CategoryGenre> domains,
        List<ContactDTO> contacts,
        @Schema(description = "Clerk ID (채팅방 생성 시 사용)", example = "user_2abc123xyz")
        String clerkId
) {}
```

---

### Task 2: MemberConverter에서 clerkId 값 세팅

**Files:**
- Modify: `devine-api/src/main/java/com/umc/devine/domain/member/converter/MemberConverter.java`

현재 `toMemberProfileDTO`는 `member` 엔티티를 직접 받지 않고 `MemberDetailDTO`만 받으므로, 호출부(`toOwnerProfileDTO`, `toOtherProfileDTO`)에서 `member.getClerkId()`를 전달해야 한다.

- [ ] **Step 1: toMemberProfileDTO 시그니처에 clerkId 파라미터 추가**

```java
private static MemberResDTO.MemberProfileDTO toMemberProfileDTO(
        MemberResDTO.MemberDetailDTO memberDTO,
        List<MemberCategory> memberCategories,
        List<Contact> contacts,
        String clerkId
) {
    List<CategoryGenre> domains = memberCategories.stream()
            .map(mc -> mc.getCategory().getGenre())
            .collect(Collectors.toList());

    List<MemberResDTO.ContactDTO> contactDTOs = contacts.stream()
            .map(contact -> MemberResDTO.ContactDTO.builder()
                    .type(contact.getContactType())
                    .value(contact.getValue())
                    .link(contact.getLink())
                    .build())
            .collect(Collectors.toList());

    return MemberResDTO.MemberProfileDTO.builder()
            .member(memberDTO)
            .domains(domains)
            .contacts(contactDTOs)
            .clerkId(clerkId)
            .build();
}
```

- [ ] **Step 2: toOwnerProfileDTO에서 clerkId 전달**

```java
public static MemberResDTO.MemberProfileDTO toOwnerProfileDTO(
        Member member,
        List<MemberCategory> memberCategories,
        List<Contact> contacts
) {
    return toMemberProfileDTO(toOwnerDetailDTO(member), memberCategories, contacts, member.getClerkId());
}
```

- [ ] **Step 3: toOtherProfileDTO에서 clerkId 전달**

```java
public static MemberResDTO.MemberProfileDTO toOtherProfileDTO(
        Member member,
        List<MemberCategory> memberCategories,
        List<Contact> contacts
) {
    return toMemberProfileDTO(toOtherDetailDTO(member), memberCategories, contacts, member.getClerkId());
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew :devine-api:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add devine-api/src/main/java/com/umc/devine/domain/member/dto/MemberResDTO.java
git add devine-api/src/main/java/com/umc/devine/domain/member/converter/MemberConverter.java
git commit -m "feat: expose clerkId in MemberProfileDTO for chat initiation"
```

---

### Task 3: UpdateProjectRes에 creatorClerkId 추가

**Files:**
- Modify: `devine-api/src/main/java/com/umc/devine/domain/project/dto/ProjectResDTO.java:80-140`

- [ ] **Step 1: UpdateProjectRes record에 creatorClerkId 필드 추가**

기존 `creatorImage` 바로 아래에 추가:

```java
@Schema(description = "프로젝트 생성자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
String creatorImage,

@Schema(description = "프로젝트 생성자 Clerk ID (채팅방 생성 시 사용)", example = "user_2abc123xyz")
String creatorClerkId,
```

---

### Task 4: ProjectConverter에서 creatorClerkId 값 세팅

**Files:**
- Modify: `devine-api/src/main/java/com/umc/devine/domain/project/converter/ProjectConverter.java:94-120`

- [ ] **Step 1: toUpdateProjectRes에 creatorClerkId 추가**

기존 `.creatorImage(...)` 바로 아래에 추가:

```java
return ProjectResDTO.UpdateProjectRes.builder()
        .projectId(project.getId())
        .projectField(project.getProjectField())
        .projectFieldName(project.getProjectField().getDisplayName())
        .category(project.getCategory().getGenre())
        .categoryName(project.getCategory().getGenre().getDisplayName())
        .mode(project.getMode())
        .modeName(project.getMode().getDisplayName())
        .durationRange(project.getDurationRange())
        .durationRangeName(project.getDurationRange().getDisplayName())
        .location(project.getLocation())
        .recruitmentDeadline(project.getRecruitmentDeadline())
        .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
        .title(project.getTitle())
        .content(project.getContent())
        .status(project.getStatus())
        .creatorId(project.getMember().getId())
        .creatorNickname(project.getMember().getNickname())
        .creatorImage(project.getMember().getImage())
        .creatorClerkId(project.getMember().getClerkId())
        .recruitments(toRecruitmentInfoList(project.getRequirements(), techstackMap))
        .images(toImageInfoList(project.getImages()))
        .build();
```

- [ ] **Step 2: 빌드 확인**

```bash
./gradlew :devine-api:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add devine-api/src/main/java/com/umc/devine/domain/project/dto/ProjectResDTO.java
git add devine-api/src/main/java/com/umc/devine/domain/project/converter/ProjectConverter.java
git commit -m "feat: expose creatorClerkId in UpdateProjectRes for chat initiation"
```
