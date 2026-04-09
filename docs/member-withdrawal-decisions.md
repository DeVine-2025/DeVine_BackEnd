# 회원 탈퇴 설계 결정 기록

이 문서는 회원 탈퇴 기능을 구현하면서 내린 설계 결정과 그 이유를 기록한다.

---

## 1. Soft Delete + Hard Delete 2단계 구조를 선택한 이유

### 결정

탈퇴 요청 시 즉시 행을 삭제하지 않고, 상태를 `DELETED`로 변경(Soft Delete)한 뒤 일정 기간 후 배치로 완전 삭제(Hard Delete)한다.

### 이유

- **복구 가능성** — 실수로 탈퇴한 회원이 복구를 요청할 수 있는 유예 기간을 확보한다 (기본 30일)
- **비즈니스 데이터 정합성** — payment, matching, project, chat 등 다른 회원과 연결된 비즈니스 레코드는 즉시 삭제할 수 없다. 참조 무결성을 깨뜨리지 않으려면 member 행이 남아 있어야 한다
- **외부 시스템 정합성** — Clerk 삭제 API가 실패하더라도 DB는 이미 익명화되어 있으므로 개인정보 유출 위험이 없다. 반대로 즉시 삭제 후 Clerk 호출이 실패하면 DB에는 데이터가 없는데 Clerk에는 계정이 남는 불일치가 생긴다

---

## 2. 탈퇴 즉시 PII를 익명화하는 이유

### 결정

Hard Delete를 기다리지 않고, 탈퇴 시점에 즉시 Member의 개인정보 필드(name, nickname, address, image, body, githubUsername)를 `null` 또는 고정값으로 덮어쓴다.

### 이유

- **개인정보 보호** — Hard Delete까지 최대 30일이 걸린다. 그 사이에 DB 접근 권한이 있는 사람이 개인정보를 조회할 수 있는 상태로 두면 안 된다
- **API 노출 방지** — Soft Delete 상태에서도 다른 회원이 프로필을 조회하는 쿼리에 우연히 포함될 수 있다. 익명화된 데이터만 남기면 노출되어도 식별이 불가능하다

---

## 3. PII 연관 데이터(contact, git_repo_url 등)를 탈퇴 시 즉시 삭제하는 이유

### 결정

Member 컬럼만 익명화하는 것이 아니라, 연락처(contact), GitHub 레포(git_repo_url), 분석 리포트(dev_report, report_embedding), 기술스택(dev_techstack) 행까지 탈퇴 시점에 삭제한다.

### 이유

- **Member 익명화만으로는 부족** — contact 테이블에 이메일과 LinkedIn URL이, git_repo_url에 GitHub 레포지토리 주소가 남아 있으면 탈퇴 회원을 특정할 수 있다
- **FK 안전성** — 이 데이터들은 다른 회원이 참조하지 않는 순수 개인 데이터이므로, 즉시 삭제해도 다른 비즈니스 로직에 영향이 없다
- **Hard Delete 부담 감소** — 탈퇴 시점에 PII를 미리 정리하면 Hard Delete 배치가 처리할 데이터가 줄어든다

---

## 4. clerkId를 null이 아닌 `deleted-{UUID}`로 설정하는 이유

### 결정

```java
this.clerkId = "deleted-" + UUID.randomUUID();
```

### 이유

- **unique 제약 보호** — clerkId에 unique 제약이 걸려 있다. `null`로 설정하면 DB에 따라 NULL 중복을 허용하거나 허용하지 않는 동작이 다르다. UUID를 붙인 고유 문자열로 대체하면 모든 DB에서 안전하다
- **디버깅 편의** — `"deleted-"` prefix가 있으면 로그나 DB 조회 시 탈퇴 회원임을 즉시 식별할 수 있다

---

## 5. Clerk 삭제를 트랜잭션 커밋 후 비동기로 처리하는 이유

### 결정

`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`로 Clerk API를 호출한다.

### 이유

- **DB 롤백 시 Clerk 삭제 방지** — 트랜잭션 내에서 Clerk를 먼저 삭제하면, 이후 DB 작업에서 예외가 발생해 롤백되었을 때 Clerk 계정만 삭제되고 DB에는 회원이 남는 불일치가 생긴다. 커밋 후에 호출하면 DB 변경이 확정된 상태에서만 외부 API를 호출한다
- **응답 속도** — Clerk API 호출은 네트워크 I/O가 필요하다. 비동기로 처리하면 사용자에게 즉시 응답을 반환할 수 있다
- **Clerk 실패 허용** — Clerk 삭제가 실패하더라도 DB에는 이미 익명화가 완료되어 있으므로 개인정보 유출 위험이 없다. Clerk 쪽에 비활성 계정이 남을 뿐이다

---

## 6. Clerk 404 응답을 정상 처리하는 이유

### 결정

```java
if (statusCode == 404) {
    log.info("[Clerk] 사용자가 이미 삭제됨 - clerkId={}", clerkId);
    return; // 이미 삭제된 상태이므로 정상 처리
}
```

### 이유

- **멱등성 보장** — 네트워크 장애로 첫 번째 호출의 응답을 받지 못한 뒤 재시도하면, Clerk에서는 이미 삭제 완료된 상태이므로 404를 반환한다. 이를 오류로 처리하면 불필요한 재시도 루프에 빠진다
- **운영 안정성** — 관리자가 Clerk 대시보드에서 수동으로 사용자를 먼저 삭제한 경우에도 배치/이벤트 리스너가 정상 동작한다

---

## 7. Derived Delete 대신 JPQL Bulk Delete를 사용하는 이유

### 결정

```java
// 사용하지 않음 (N+1)
void deleteAllByMember(Member member);

// 실제 사용
@Modifying
@Query("DELETE FROM Contact c WHERE c.member = :member")
int bulkDeleteByMember(@Param("member") Member member);
```

### 이유

- **N+1 문제** — Spring Data의 derived delete(`deleteAllByMember`)는 내부적으로 `SELECT` 전체 조회 후 건별 `DELETE`를 실행한다. 연락처가 10개면 SELECT 1회 + DELETE 10회 = 11회 쿼리가 발생한다
- **성능** — JPQL `@Modifying` 벌크 삭제는 `DELETE FROM contact WHERE member_id = ?` 단일 쿼리로 실행된다. 탈퇴 시 여러 테이블을 동시에 정리해야 하므로 쿼리 수가 중요하다

---

## 8. FK 삭제 순서를 지정하는 이유

### 결정

```
report_embedding → dev_report → git_repo_url → (이후 나머지)
```

### 이유

- **FK 참조 무결성** — `report_embedding`이 `dev_report`를 참조하고, `dev_report`가 `git_repo_url`을 참조한다. `git_repo_url`을 먼저 삭제하면 `dev_report`의 FK가 깨지면서 `DataIntegrityViolationException`이 발생한다
- **자식 먼저 삭제** — FK 체인의 가장 말단(자식)부터 역순으로 삭제해야 부모를 안전하게 삭제할 수 있다

---

## 9. Hard Delete 스케줄러에서 행마다 별도 트랜잭션을 사용하는 이유

### 결정

```java
for (Long id : ids) {
    Boolean ok = writeTx.execute(status -> {
        try { ... } 
        catch (DataIntegrityViolationException e) {
            status.setRollbackOnly();
        }
    });
}
```

### 이유

- **부분 실패 격리** — 하나의 트랜잭션에서 100명을 삭제하다가 1명이 FK 위반으로 실패하면, 나머지 99명의 삭제도 롤백된다. 행별 트랜잭션이면 실패한 1명만 건너뛰고 나머지는 정상 삭제된다
- **비즈니스 레코드 잔존 대응** — payment, matching, project, chat 등은 삭제 대상이 아니다. 이 레코드가 남아 있는 회원은 FK 위반으로 삭제가 실패하며, 건너뛴 뒤 로그를 남겨 운영자가 사후 처리할 수 있게 한다

---

## 10. Hard Delete 스케줄러를 기본 비활성화하는 이유

### 결정

```java
@ConditionalOnProperty(name = "member.hard-delete.enabled", havingValue = "true")
```

### 이유

- **안전 장치** — Hard Delete는 되돌릴 수 없는 파괴적 작업이다. 개발/스테이징 환경에서 의도치 않게 데이터가 삭제되는 것을 방지하기 위해, 운영 환경에서만 명시적으로 활성화하도록 설계한다
- **점진적 배포** — 기능을 먼저 배포한 뒤, 운영팀이 준비되었을 때 설정값 변경만으로 활성화할 수 있다

---

## 11. Hard Delete에서 네이티브 쿼리를 사용하는 이유

### 결정

```java
@Modifying @Query(value = "DELETE FROM contact WHERE member_id = :id", nativeQuery = true)
int hardDeleteContactsOf(@Param("id") Long id);
```

### 이유

- **영속성 컨텍스트 우회** — Hard Delete 스케줄러는 `TransactionTemplate`으로 별도 트랜잭션을 관리한다. JPQL 벌크 삭제를 사용하면 영속성 컨텍스트에 캐시된 엔티티와 불일치가 생길 수 있다. 네이티브 쿼리는 JPA를 완전히 우회하므로 이 문제가 없다
- **성능** — 엔티티를 로딩하지 않고 DB에서 직접 삭제하므로 메모리 사용량과 쿼리 수가 최소화된다
- **단순성** — 삭제 후 해당 엔티티를 다시 읽을 일이 없으므로, 영속성 컨텍스트 정합성을 신경 쓸 필요가 없다

---

## 12. payment, matching, project, chat을 삭제하지 않는 이유

### 결정

Hard Delete 시 이 테이블들은 삭제 대상에 포함하지 않고, FK 위반이 발생하면 해당 회원을 건너뛴다.

### 이유

- **비즈니스 연속성** — 결제 내역(payment)은 세무/감사 목적으로 보관해야 한다. 프로젝트(project)와 매칭(matching)은 다른 회원들과 연결되어 있으므로 한쪽 탈퇴로 삭제하면 상대방 데이터가 사라진다
- **채팅 이력** — 상대방 입장에서 대화 기록이 갑자기 사라지는 것은 UX상 바람직하지 않다. 탈퇴 회원은 이미 익명화("deleted")되어 있으므로 기록은 남되 누구인지 알 수 없는 상태가 된다
- **법적 요건** — 결제 관련 데이터는 전자상거래법 등에 의해 일정 기간 보관 의무가 있을 수 있다

---

## 13. @SQLRestriction("used = 'ACTIVE'")을 적용하지 않은 이유

### 검토 내용

Hibernate의 `@SQLRestriction`을 Member 엔티티에 적용하면 모든 조회에 자동으로 `WHERE used = 'ACTIVE'` 조건이 붙어, 탈퇴 회원이 조회되지 않는다.

### 적용하지 않은 이유

- **@ManyToOne JOIN FETCH 파괴** — ChatRoom, Matching, Project 등이 `@ManyToOne Member`를 가지고 있는데, `@SQLRestriction`이 걸리면 DELETED 상태의 member를 참조하는 행의 JOIN FETCH가 조용히 결과에서 제외된다. 에러 없이 데이터가 누락되므로 디버깅이 매우 어렵다
- **L1 캐시 불일치** — `em.find()` (JPA `findById`)는 영속성 컨텍스트 L1 캐시에 엔티티가 있으면 `@SQLRestriction`을 무시하고 캐시된 값을 반환한다. 같은 트랜잭션 내에서 조회 결과가 일관되지 않는다
- **기존 쿼리로 충분** — 현재 MemberRepository의 모든 조회 쿼리에 이미 `m.used = 'ACTIVE'` 조건이 명시되어 있어, `@SQLRestriction` 없이도 탈퇴 회원이 일반 조회에 노출되지 않는다

---

## 14. MemberStatus.INACTIVE를 제거한 이유

### 결정

`ACTIVE`, `INACTIVE`, `DELETED` 3개 상태에서 `INACTIVE`를 제거하고 `ACTIVE`, `DELETED` 2개만 남긴다.

### 이유

- **미사용 상태** — `INACTIVE`는 과거 가입 플로우(빈 user row 생성 → 가입 완료 시 ACTIVE 전환)에서 사용되던 상태이며, 현재 가입 플로우에서는 사용되지 않는다
- **데이터 부재** — DB 확인 결과 `used = 'INACTIVE'`인 회원 데이터가 존재하지 않는다
- **탈퇴 로직 단순화** — 상태가 2개만 있으면 "ACTIVE가 아닌 모든 회원은 탈퇴 회원"이라는 단순한 규칙으로 처리할 수 있다
