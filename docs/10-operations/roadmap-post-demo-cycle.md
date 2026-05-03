# Roadmap — Post Demo Cycle (v0.1)

- Date: `2026-05-03`
- Trigger: `demo-cycle-001` 완료 (PR #4) + E2E 16/16 검증 통과
- Source: `docs/10-operations/tasks/task-queue.yaml`

---

## 0. 현재 상태 스냅샷

### ✅ 완료
| ID | 산출물 | PR |
|:---|:---|:---|
| `po-strategy-sync-001` | PRD/Strategy/Priority 문서 동결 | merged |
| `po-platform-direction-review-001` | YouTube Music 검토 → Apple 유지 | merged |
| `pd-ux-flow-001` | user-flows-v0.1.md, screen-specs-v0.1.md | #4 |
| `be-adapter-arch-001` | adapter-architecture-v0.1.md, canonical-track-normalization-v0.1.md | #4 |
| `demo-cycle-001` | BE H2 데모 + FE 9-route + E2E Playwright 16/16 | #4 (open) |

### 🟡 사용자 검증된 동작
- Spotify Import → 정규화 → Asset 생성 (6 트랙)
- Emotional Context 부착 (diaryText + emotionTags)
- Apple Music Export → partial 매칭 5/6 → 결과 페이지
- 공유 토큰 발급 → 익명 `/share/[token]` 접근

### 🔴 알려진 제약
- 모든 외부 API가 Mock 어댑터로 대체됨
- 사용자 인증 없음 (Spring Security permitAll)
- DB는 H2 인메모리 (재시작 시 소실)
- CI/CD 미구성

---

## 1. 우선순위 매트릭스

| 우선 | 차원 | 태스크 | 이유 |
|:---:|:---|:---|:---|
| **P1** | 외부 통합 | `be-spotify-oauth-001` | 실제 사용자 데이터 가져와야 가치 검증 가능 |
| **P1** | 외부 통합 | `be-apple-musickit-001` | 실제 익스포트 없으면 핵심 가설 검증 불가 |
| **P1** | 데이터 무결성 | `be-matching-engine-001` | ISRC 매칭 품질이 곧 제품 신뢰도 |
| **P1** | 사용자 모델 | `fe-auth-flow-001` | OAuth 도입 시 세션 관리 필수 |
| **P1** | 정책/저장 | `be-emotional-context-media-001` | 사진 첨부 = Emotional Context 핵심 입력 |
| **P1** | 운영 | `ops-postgres-prod-001` | 데이터 영속화 없이는 베타 불가 |
| **P1** | 바이럴 | `fe-share-page-seo-001` | 공유 K-factor 측정에 필수 |
| **P2** | 자동화 | `ops-ci-cd-001` | 멀티에이전트 워크플로우의 안전망 |

---

## 2. 의존성 그래프

```
demo-cycle-001 ✅
       │
       ├─► be-spotify-oauth-001 ──────┐
       │                              │
       │                              ▼
       │                       fe-auth-flow-001
       │
       ├─► be-apple-musickit-001 (병렬 가능)
       │
       ├─► be-matching-engine-001 (병렬 가능)
       │
       ├─► be-emotional-context-media-001 (병렬 가능)
       │
       ├─► fe-share-page-seo-001 (병렬 가능)
       │
       ├─► ops-postgres-prod-001 (병렬 가능)
       │
       └─► ops-ci-cd-001 (병렬 가능, P2)
```

**핵심 발견**: `demo-cycle-001` 머지 후 6개 태스크가 병렬 실행 가능 → 멀티에이전트로 1 사이클에 P1 절반 처리 가능.

---

## 3. 권장 실행 순서 (3 사이클)

### 🎯 Cycle 2 — "Realness wave" (병렬 4개)

| 동시 실행 | 에이전트 | 산출물 |
|:---|:---|:---|
| `be-spotify-oauth-001` | codex-cli | RealSpotifyClient + AuthController |
| `be-apple-musickit-001` | codex-cli | RealAppleMusicAdapter + JWT 서명 |
| `be-emotional-context-media-001` | codex-cli | MediaController + S3 추상화 |
| `ops-postgres-prod-001` | codex-cli | Flyway 마이그레이션 + docker-compose |

**Gate**: 4개 모두 머지 + 통합 E2E (Real OAuth + Real MusicKit) 통과

### 🎯 Cycle 3 — "Trust wave" (의존 + 병렬 2개)

| 동시 실행 | 의존 | 산출물 |
|:---|:---|:---|
| `fe-auth-flow-001` | be-spotify-oauth-001 | NextAuth + 보호 라우트 |
| `be-matching-engine-001` | (없음, 병렬) | 실제 fuzzy 매칭 |
| `fe-share-page-seo-001` | (없음, 병렬) | OG + Twitter 카드 |

**Gate**: 인증된 사용자가 실제 Spotify에서 가져와 실제 Apple로 export하면서 매칭 품질을 보장

### 🎯 Cycle 4 — "Operational hardening"

| 태스크 | 산출물 |
|:---|:---|
| `ops-ci-cd-001` | GitHub Actions + Playwright in CI |
| (post-MVP) `be-proof-of-human-001` | 인간성 검증 시그널 (P1 → MVP 후 강화) |
| (post-MVP) `pd-tier-1-paywall-001` | 프리미엄 익스포트 BM 디자인 |

---

## 4. 검증 게이트 (각 사이클)

### Acceptance: real-data E2E
1. 본인 Spotify 계정으로 OAuth 로그인
2. 실제 플레이리스트 1개 import
3. ISRC 매칭률 ≥ 70% 달성
4. Emotional Context (diary + 사진 1장 + 태그) 부착
5. Apple Music 실제 라이브러리에 export 완료
6. matchedTracks 카운트 BE/FE/Apple 3자 일치
7. 공유 링크 → OG 카드가 메신저에 정상 표시

### 측정 메트릭 (KPI)
| 메트릭 | 목표 | 출처 |
|:---|:---|:---|
| Import → Export 전환율 | ≥ 60% | BE export job count / asset count |
| Context Density | ≥ 40% PA에 diary 또는 사진 포함 | Asset.diaryText/photoUrls |
| 매칭 정확도 | ISRC 기반 ≥ 90% | matchConfidence 분포 |
| 공유 → 가입 (K-factor) | ≥ 0.3 | /share/{token} → signup 추적 |

---

## 5. 리스크 & 완화

| 리스크 | 가능성 | 영향 | 완화 |
|:---|:---:|:---:|:---|
| Spotify API 정책 변경 | 중 | 치명 | quota 모니터링 + Mock 폴백 항상 유지 |
| Apple MusicKit 토큰 발급 지연 | 중 | 높음 | 비동기 익스포트 큐 + 재시도 |
| ISRC 누락 트랙 처리 | 높음 | 중 | fuzzy 매칭 + 사용자 수동 매칭 UX (P2) |
| H2 → Postgres 데이터 마이그레이션 | 낮음 | 중 | Flyway baseline + JPA 스키마 일치 검증 |
| 공유 페이지 악용 (스팸) | 낮음 | 낮음 | shareToken rate limit + revoke API |

---

## 6. 멀티에이전트 워크플로우 다음 진화

### 6.1 현재 입증된 패턴 (이번 사이클에서)
- 4개 에이전트 병렬 가능 (PD+BE 문서 → BE+FE 구현)
- E2E 검증 자동화 (Playwright 16/16)
- BE race condition 같은 통합 이슈 발견은 결국 사람의 수동 디버깅 필요

### 6.2 다음 사이클에서 시도할 것
1. **Acceptance test 우선 작성**: 에이전트 디스패치 전에 E2E 시나리오를 작성해서 에이전트가 자체 verification loop 돌리게
2. **Worktree 격리**: 4개 BE/FE 태스크 병렬 시 git worktree로 충돌 방지
3. **BLOCKING_QUESTION 자동 감지**: 에이전트가 "OAuth scope를 어디까지?" 같은 질문을 로그에 남기면 orchestrator가 사람 알림 발송

---

## 7. 즉시 실행 가능한 액션

```zsh
# 1. PR #4 머지
gh pr merge 4 --squash --delete-branch

# 2. 큐 동기화 → P1 wave가 awaiting_approval로 이동
zsh .claude/scripts/sync-queue.sh

# 3. 다음 승인 대기 브리핑 확인
zsh .claude/scripts/propose-decisions.sh

# 4. Cycle 2 (4개 병렬) 일괄 승인
zsh .claude/scripts/approve.sh be-spotify-oauth-001
zsh .claude/scripts/approve.sh be-apple-musickit-001
zsh .claude/scripts/approve.sh be-emotional-context-media-001
zsh .claude/scripts/approve.sh ops-postgres-prod-001

# 5. Worker / 멀티에이전트 디스패치
# (현재처럼 Claude 안에서 4개 Agent를 병렬 spawn 하거나
#  worker.sh codex-cli 4 인스턴스로 분산 처리)
```
