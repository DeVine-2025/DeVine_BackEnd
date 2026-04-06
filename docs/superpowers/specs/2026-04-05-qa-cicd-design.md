# QA 브랜치 CI/CD 파이프라인 설계

## 개요

기존 `main` 브랜치 → AWS(운영) CI/CD 파이프라인에 `qa` 브랜치 → OCI(개발) 배포를 추가한다.
기존 4개 워크플로우 파일에 브랜치 분기를 추가하는 방식(방식 B)을 채택한다.

## 환경 구성

| 항목 | 운영 (prod) | 개발 (qa) |
|------|------------|-----------|
| 브랜치 | `main` | `qa` |
| 인프라 | AWS EC2 | OCI Compute Instance |
| 배포 방식 | SSH + docker compose | SSH + docker compose |
| Docker 레지스트리 | Docker Hub | Docker Hub (태그 분리) |
| Spring 프로필 | `prod` | `prod` (.env로 환경 주입) |
| .env 소스 | S3 `.env.prod` | S3 `.env.qa` |

## CI 워크플로우 변경 (ci-api.yml, ci-realtime.yml)

### 트리거

```yaml
on:
  push:
    branches: [main, qa]
```

paths 필터는 기존과 동일하게 유지.

### Docker 이미지 태그 분기

- `main` 브랜치: `{sha}`, `latest` (기존 그대로)
- `qa` 브랜치: `qa-{sha}`, `qa-latest`

`github.ref_name` 으로 분기 처리.

## CD 워크플로우 변경 (cd-api.yml, cd-realtime.yml)

### 트리거

```yaml
on:
  workflow_run:
    branches: [main, qa]
```

### Job 분리

기존 `deploy` job을 `deploy-prod`와 `deploy-qa`로 분리.

#### deploy-prod

- 조건: `github.event.workflow_run.head_branch == 'main'`
- SSH 대상: EC2 (`EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY`, `EC2_SSH_PORT`)
- .env: `aws s3 cp s3://{bucket}/config/.env.prod .env`
- Docker 태그: `{sha}`, `latest`
- 헬스체크 + 롤백: 기존과 동일

#### deploy-qa

- 조건: `github.event.workflow_run.head_branch == 'qa'`
- SSH 대상: OCI (`OCI_HOST`, `OCI_USERNAME`, `OCI_SSH_KEY`, `OCI_SSH_PORT`)
- .env: `aws s3 cp s3://{bucket}/config/.env.qa .env`
- Docker 태그: `qa-{sha}`, `qa-latest`
- 헬스체크 + 롤백: prod와 동일한 로직 적용

## 필요한 사전 작업

### GitHub Secrets 추가

- `OCI_HOST`: OCI 인스턴스 IP
- `OCI_USERNAME`: SSH 사용자명
- `OCI_SSH_KEY`: SSH 프라이빗 키
- `OCI_SSH_PORT`: SSH 포트

### S3 .env 파일 변경

- 기존 `.env` → `.env.prod`로 이름 변경
- `.env.qa` 파일 새로 생성 (qa 환경용 DB, API 키 등)

### OCI 인스턴스 준비

- AWS CLI 설치 및 자격증명 설정 (S3 접근용)
- Docker + docker compose 설치
- `~/Devine_Deploy/service/docker-compose.yml` 세팅 (완료됨)

## Dockerfile 변경

변경 없음. `prod` 프로필 그대로 사용하며 `.env`로 환경별 값을 주입한다.

## 수정 대상 파일

1. `.github/workflows/ci-api.yml` — 트리거 브랜치 추가, Docker 태그 분기
2. `.github/workflows/ci-realtime.yml` — 동일
3. `.github/workflows/cd-api.yml` — 트리거 브랜치 추가, deploy job 분리
4. `.github/workflows/cd-realtime.yml` — 동일
