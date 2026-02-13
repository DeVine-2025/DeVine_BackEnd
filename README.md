<div align="center">

# DeVine Backend

**개발자와 PM을 연결하는 IT 협업 커뮤니티 플랫폼**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Valkey](https://img.shields.io/badge/Valkey-FF4438?logo=redis&logoColor=white)](https://valkey.io/)
[![AWS](https://img.shields.io/badge/AWS-FF9900?logo=amazonwebservices&logoColor=white)](https://aws.amazon.com/)
[![JPA](https://img.shields.io/badge/JPA-59666C?logo=hibernate&logoColor=white)](https://spring.io/projects/spring-data-jpa)
[![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD?logoColor=white)](http://querydsl.com/)
[![JUnit5](https://img.shields.io/badge/JUnit5-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Nginx](https://img.shields.io/badge/Nginx-009639?logo=nginx&logoColor=white)](https://nginx.org/)
[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![Clerk](https://img.shields.io/badge/Clerk-6C47FF?logo=clerk&logoColor=white)](https://clerk.com/)

</div>

---

## 📌 Project Overview

**DeVine(디바인)** 은 사이드 프로젝트를 함께할 개발자와 PM을 매칭해주는 IT 협업 커뮤니티 플랫폼입니다.

프로젝트 등록부터 팀원 매칭, AI 기반 프로젝트 분석 리포트까지 — 사이드 프로젝트의 시작을 체계적으로 지원합니다.

### 🎯 핵심 가치

| 가치 | 설명 |
|------|------|
| **스마트 매칭** | 기술 스택·관심 분야 기반 팀원 매칭 시스템 |
| **AI 분석** | FastAPI AI 워커를 통한 프로젝트 분석 리포트 자동 생성 |
| **실시간 알림** | SSE(Server-Sent Events) + Redis Pub/Sub 기반 실시간 알림 |
| **GitHub 연동** | GitHub 프로필 및 레포지토리 자동 연동 |

---

## 👥 Backend Team

<div align="center">

| <img src="https://github.com/sunm2n.png" width="120" height="120" /> | <img src="https://github.com/strfunctionk.png" width="120" height="120" /> | <img src="https://github.com/KoEunB.png" width="120" height="120" /> | <img src="https://github.com/yujin9907.png" width="120" height="120" /> |
|:---:|:---:|:---:|:---:|
| [이선민](https://github.com/sunm2n) | [박수현](https://github.com/strfunctionk) | [고은비](https://github.com/KoEunB) | [허유진](https://github.com/yujin9907) |

</div>

---

## ⚡ Key Features

### 1. 🤝 프로젝트 매칭 시스템
- 프로젝트 등록 및 모집 공고 관리
- 기술 스택·카테고리 기반 필터링 검색
- 팀원 지원/수락/거절 워크플로우

### 2. 🤖 AI 분석 리포트
- FastAPI AI 워커와 비동기 연동
- GitHub 레포지토리 기반 프로젝트 분석
- 벡터 임베딩(pgvector)을 활용한 프로젝트 유사도 분석

### 3. 🔔 실시간 알림 (SSE)
- Redis Pub/Sub 기반 멀티 인스턴스 지원
- SSE(Server-Sent Events)를 통한 실시간 푸시 알림
- 전용 스레드 풀 구성으로 안정적인 커넥션 관리

### 4. 🔐 인증 및 보안
- Clerk OAuth2 기반 JWT 인증
- Spring Security + Resource Server 구성
- GitHub 소셜 로그인 지원

### 5. 📁 이미지 관리
- AWS S3 업로드 + CloudFront CDN 배포
- 프로젝트 이미지 및 프로필 이미지 관리

---

## 🛠️ Tech Stack

### Backend
| 기술 | 버전 | 용도 |
|------|------|------|
| **Java** | 21 | 메인 언어 |
| **Spring Boot** | 3.5.9 | API 서버 프레임워크 |
| **Spring Security** | 6.x | 인증/인가 |
| **Spring Data JPA** | - | ORM |
| **Hibernate** | 6.x | JPA 구현체 |
| **QueryDSL** | 7.0 | 타입 세이프 동적 쿼리 |
| **FastAPI** | - | AI 워커 서버 (Python) |

### Database & Cache
| 기술                | 용도 |
|-------------------|------|
| **PostgreSQL**    | 메인 RDBMS |
| **pgvector**      | 벡터 임베딩 저장 및 유사도 검색 |
| **Valkey(Redis)** | 캐시 및 SSE Pub/Sub 메시징 |
| **H2**            | 로컬 개발/테스트 DB |

### Infrastructure
| 기술 | 용도 |
|------|------|
| **AWS EC2** | 애플리케이션 서버 호스팅 |
| **AWS RDS** | PostgreSQL 매니지드 DB |
| **AWS S3** | 이미지 파일 스토리지 |
| **AWS CloudFront** | CDN (이미지 배포) |
| **Docker** | 컨테이너화 및 배포 |
| **GitHub Actions** | CI/CD 파이프라인 |

### Tools & Libraries
| 기술 | 용도 |
|------|------|
| **Clerk** | OAuth2 인증 서비스 |
| **Swagger (SpringDoc)** | API 문서 자동 생성 |
| **Lombok** | 보일러플레이트 코드 제거 |
| **Testcontainers** | 통합 테스트 (PostgreSQL) |
| **JUnit 5** | 단위/통합 테스트 |

---
## 🏗️ System Architecture

### 전체 시스템 구성도
![System Architecture](https://github.com/user-attachments/assets/cefce419-aecf-482d-b834-e9f0b3163fe0)

---

## 🚀 Getting Started

### Prerequisites

| 도구                | 최소 버전 | 비고 |
|-------------------|-----------|------|
| **JDK**           | 21+ | [Eclipse Temurin](https://adoptium.net/) 권장 |
| **Gradle**        | 8.x | Wrapper 포함 (`./gradlew`) |
| **Docker**        | 24.x+ | 배포 환경 구성 시 필요 |
| **PostgreSQL**    | 15+ | pgvector 확장 필요 |
| **Valkey(Redis)** | 9.0 | SSE 알림용 |

### Environment Variables

프로젝트 루트에 `.env` 파일을 생성하고 다음 환경 변수를 설정합니다:

```properties
# ===== Database =====
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DATABASE=devine
POSTGRES_USER=devine_user
POSTGRES_PASSWORD=your_password

# ===== Redis =====
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_password
REDIS_TIMEOUT=2000ms

# ===== Server =====
SPRING_PORT=8080
FAST_PORT=8000

# ===== Clerk (Authentication) =====
CLERK_SECRET_KEY=sk_test_xxxxxxxxxxxx
CLERK_ISSUER_URI=https://your-clerk-domain.clerk.accounts.dev

# ===== CORS =====
CORS_ALLOWED_ORIGINS=http://localhost:5173

# ===== AWS =====
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key
AWS_S3_BUCKET=your_bucket_name
AWS_CLOUDFRONT_DOMAIN=your_cloudfront_domain

# ===== GitHub =====
GITHUB_SERVICE_TOKEN=your_github_token

# ===== FastAPI (Production) =====
FASTAPI_URL=http://localhost:8000
FASTAPI_CALLBACK_URL=http://localhost:8080
```

### Installation & Run

```bash
# 1. 저장소 클론
git clone https://github.com/DeVine-2025/DeVine_BackEnd.git
cd DeVine_BackEnd

# 2. 환경 변수 설정
cp .env.example .env
# .env 파일을 열어 값을 설정합니다

# 3. PostgreSQL & Redis 실행 (로컬 환경)
# PostgreSQL에 pgvector 확장이 설치되어 있어야 합니다
# CREATE EXTENSION IF NOT EXISTS vector;

# 4. 빌드 및 실행
./gradlew clean build -x test
./gradlew bootRun

# 또는 Docker로 실행
docker build -t devine-backend .
docker run -p 8080:8080 --env-file .env devine-backend
```
---

## 📖 API Documentation

Swagger UI를 통해 전체 API 명세를 확인할 수 있습니다.

| 환경             | URL                                                                                        |
|----------------|--------------------------------------------------------------------------------------------|
| **Local**      | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| **Production** | [https://api.devine.kr/swagger-ui/index.html](https://api.devine.kr/swagger-ui/index.html) |
| **Test Login** | [https://api.devine.kr/dev](https://api.devine.kr/dev)                                     |

- 모든 API는 JWT Bearer 토큰 인증이 필요합니다 (일부 공개 엔드포인트 제외)
- Swagger UI 상단의 `Authorize` 버튼을 클릭하여 JWT 토큰을 입력할 수 있습니다

### 주요 API 그룹

| API | 설명 |
|-----|------|
| `/api/auth` | 인증 (로그인, 회원가입) |
| `/api/members` | 회원 정보 관리 |
| `/api/my-profile` | 내 프로필 관리 |
| `/api/projects` | 프로젝트 CRUD 및 검색 |
| `/api/matching` | 팀원 매칭 (지원/수락/거절) |
| `/api/bookmarks` | 북마크 관리 |
| `/api/notifications` | 알림 조회 및 관리 |
| `/api/reports` | AI 분석 리포트 |
| `/api/sse` | SSE 실시간 알림 연결 |
| `/api/images` | 이미지 업로드 |
| `/api/techstacks` | 기술 스택 조회 |

---

## 📂 Directory Structure

```
src/main/java/com/umc/devine/
├── 🌐 domain/                          # 도메인 계층 (비즈니스 로직)
│   ├── auth/                           # 인증 (로그인, 회원가입)
│   │   ├── controller/
│   │   ├── dto/
│   │   └── exception/
│   ├── member/                         # 회원 관리
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   ├── project/                        # 프로젝트 & 매칭
│   │   ├── controller/                 # ProjectController, MatchingController
│   │   ├── dto/
│   │   ├── entity/                     # Project, ProjectReport, ProjectEmbedding
│   │   ├── repository/
│   │   └── service/
│   ├── notification/                   # 알림
│   ├── bookmark/                       # 북마크
│   ├── report/                         # AI 리포트
│   ├── category/                       # 카테고리/장르
│   ├── techstack/                      # 기술 스택
│   ├── embedding/                      # 벡터 임베딩
│   └── image/                          # 이미지 관리
│
├── 🔧 infrastructure/                  # 인프라 계층 (외부 서비스 연동)
│   ├── clerk/                          # Clerk 인증 API 클라이언트
│   ├── fastapi/                        # FastAPI AI 워커 클라이언트
│   │   ├── FastApiEmbeddingClient      # 임베딩 요청
│   │   ├── FastApiReportClient         # 리포트 요청 (비동기)
│   │   ├── FastApiSyncReportClient     # 리포트 요청 (동기)
│   │   └── EmbeddingCallbackController # 콜백 수신
│   ├── github/                         # GitHub API 연동
│   ├── s3/                             # AWS S3 파일 업로드
│   └── sse/                            # SSE 실시간 알림
│       ├── core/                       # SSE 연결 관리
│       ├── pubsub/                     # Redis Pub/Sub
│       ├── listener/                   # 이벤트 리스너
│       └── controller/                 # SSE 엔드포인트
│
├── ⚙️ global/                          # 공통 설정 및 유틸리티
│   ├── config/                         # 설정 (Security, Redis, Swagger, QueryDSL 등)
│   ├── auth/                           # 인증 필터 및 핸들러
│   ├── apiPayload/                     # 공통 응답 래퍼
│   ├── entity/                         # BaseEntity (Auditing)
│   ├── dto/                            # 공통 DTO
│   ├── enums/                          # 공통 Enum
│   ├── scheduler/                      # 스케줄러
│   ├── util/                           # 유틸리티 클래스
│   └── validation/                     # 커스텀 Validation
│
└── DeVineApplication.java              # 애플리케이션 진입점
```

---

## 📚 Documentation

프로젝트 내 `docs/` 폴더에서 상세 기술 문서를 확인할 수 있습니다:

| 문서 | 설명 |
|------|------|
| [auth-integration-guide.md](docs/auth-integration-guide.md) | 인증 연동 가이드 |
| [clerk-jwt-implementation.md](docs/clerk-jwt-implementation.md) | Clerk JWT 구현 상세 |
| [github-api-integration-guide.md](docs/github-api-integration-guide.md) | GitHub API 연동 가이드 |
| [sse-notification-guide.md](docs/sse-notification-guide.md) | SSE 실시간 알림 구현 가이드 |
| [integration-test-guide.md](docs/integration-test-guide.md) | 통합 테스트 가이드 |

---

## 🌿 Git Branch Strategy

### 커밋 타입

| 타입 | 설명 |
|------|------|
| `FEATURE` | 새로운 기능 추가 |
| `FIX` | 버그 수정 |
| `HOTFIX` | 긴급 버그 수정 |
| `REFACTOR` | 코드 리팩토링 (기능 변경 없음) |
| `CHORE` | 빌드, 설정, 의존성 등 기타 변경 |
| `TEST` | 테스트 코드 추가/수정 |
| `DOCS` | 문서 추가/수정 |

### 커밋 메시지 규칙

```
[TYPE] 변경 내용 요약
```

**예시:**
```
[FEATURE] 매칭 상태 조회 API 구현
[FIX] Swagger 요청 DTO enum 예시값 불일치 수정 (#106)
[REFACTOR] 회원 도메인 API 구조 개선 및 GitHub 연동 기능 고도화 (#99)
[HOTFIX] import 경로 업데이트
```

- 타입은 **대문자**로 작성하며 대괄호`[]`로 감쌉니다
- 관련 이슈가 있으면 끝에 `(#이슈번호)`를 붙입니다
- 한글로 간결하게 작성합니다

### 브랜치 네이밍 규칙

```
<type>/#<issue-number>-<short-description>
```

| 브랜치 유형 | 패턴 | 예시 |
|------------|------|------|
| 기능 개발 | `feat/#<이슈번호>-<설명>` | `feat/#102-get-matching` |
| 버그 수정 | `fix/#<이슈번호>-<설명>` | `fix/#125-report` |
| 리팩토링 | `refactor/#<이슈번호>-<설명>` | `refactor/#96-sync-report-v2` |
| 설정/기타 | `chore/#<이슈번호>-<설명>` | `chore/#22-setting-valkey` |
| 테스트 | `test/#<이슈번호>-<설명>` | `test/#115-edit-test-project` |
| 문서 | `docs/#<이슈번호>-<설명>` | `docs/#138-add-readme` |
| 긴급 수정 | `hotfix/#<이슈번호>-<설명>` | `hotfix/#158-matching-valid-score` |

### 브랜치 전략 (Git Flow)

```
main (프로덕션)
 └── dev (개발 통합)
      ├── feat/#10-project-api
      ├── fix/#125-report
      ├── refactor/#96-sync-report-v2
      └── ...
```

| 브랜치 | 역할 | 보호 규칙 |
|--------|------|----------|
| `main` | 프로덕션 배포 브랜치 | PR 머지만 허용, 직접 푸시 금지 |
| `dev` | 개발 통합 브랜치 (기본 브랜치) | PR 머지만 허용, 직접 푸시 금지 |
| `feat/*`, `fix/*` 등 | 작업 브랜치 | `dev`에서 분기 → `dev`로 PR |

### PR (Pull Request) 규칙

- PR 제목은 커밋 메시지 규칙과 동일하게 `[TYPE] 변경 내용 요약` 형식을 따릅니다
- 관련 이슈를 연결합니다
- 코드 리뷰 승인 후 머지합니다

### 머지 전략

| 대상 | 전략 | 이유 |
|------|------|------|
| 작업 브랜치 → `dev` | **Squash and Merge** | 작업 브랜치의 커밋을 하나로 합쳐 깔끔한 히스토리 유지 |
| `dev` → `main` | **Merge Commit** | 배포 단위별 히스토리 보존 |

---

<div align="center">

**DeVine** · Built with ☕ by [DeVine Team](https://github.com/DeVine-2025)

</div>
