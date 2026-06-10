# Roadmap v2 — Social Pivot (중간점검 반영)

- Date: `2026-06-10`
- Trigger: CEO 중간점검 — YTM 코리도 우선순위 재검토 + 소셜 레이어(랭킹/선물/피드) 복원 지시
- 근거 문서:
  - PO 재검토: `docs/20-product/strategy/platform-strategy-v2-research-v0.1.md` (v0.1 결정 공식 대체)
  - PD 리서치: `docs/20-product/design/ux/social-ux-research-v0.1.md`, `user-flows-v0.2-social.md`
- Supersedes: `roadmap-post-demo-cycle.md`의 잔여 항목

---

## 0. 중간점검 스냅샷 (2026-06-10)

### 잘된 점
- 프론트 "Nocturne" 다크 리디자인 (PR #10) — Pretendard, 앨범아트 전면, E2E 16/16 유지
- 백엔드 polyenm_pan 컨벤션 도입 (PR #11~13) — 예외 표준화, package-by-feature, ApiResponse envelope + Swagger

### 못한 점 (CEO 지적)
- 실제 소셜 로그인 미연동 (PKCE 코드만 존재, 실 자격증명/계정 모델 없음)
- YTM↔Spotify / YTM↔Apple 코리도 미구현 (인지도 상 우선순위 높음)
- "딸깍" 원클릭 전환 직관성 부족 (가져오기→자산화→내보내기 다단계)

### 숨은 부채 (플랜 반영 필수)
- MatchingEngine ↔ NormalizationEngine 미통합 (canonical track이 import에서 안 만들어짐)
- 소셜 기능의 전제인 **User 계정 모델 부재** (현재 익명 grantId 쿠키)
- H2 인메모리 → 소셜 데이터는 영속화 필요 (Postgres 전환 시점 도래)
- S3 SigV4 stub, CI 비활성 상태

---

## 1. PO 재검토 핵심 결정 (Freeze, v0.2)

| # | 결정 | 함의 |
|:--|:--|:--|
| 1 | **YTM = P0 플랫폼** (Spotify↔YTM 코리도 최우선) | v0.1(Apple 유지) 공식 대체. 단 **Phase 1은 '가져오기+공유'만** |
| 2 | **Google OAuth 메인 인증** | 소셜 가입 + YouTube scope를 한 번에 (Incremental Auth). Hub & Spoke: 가입=Google, Spotify/Apple은 마이페이지 추가 연동 |
| 3 | **"가져오기 → 소셜 피드 공유" 최우선 UX 경로** | 쿼터 비싼 export 대신 import(1 unit)+피드로 유입 극대화 |
| 4 | **Export는 '선택적 개방'** | YT 쿼터 수학: 30곡 내보내기 ≈ 1,550 units → 기본 쿼터로 **일 6.4건**. 랭킹 상위/포인트제 개방 → 실측 확보 → 쿼터 증설(100만+) 신청 |

소셜 MVP 컷 (PO): **P0** = Google 로그인·팔로잉 피드·좋아요·댓글·공유 / **P1** = 랭킹·무드 추천·선물 기초 / **P2** = DM·감상방·외부 SNS 자동 포스팅

videoId 정규화: ISRC 미존재(cover/fan-upload) 시 fuzzy 2순위 + **Low Confidence 플래그 → 사용자 확인 UX**.

---

## 2. 단계별 작업 플랜 (큐 task id 매핑)

### Phase 0 — 리서치 ✅ (이번 사이클)
| task | 상태 |
|:--|:--|
| `po-strategy-v2-001` (Gemini) | done — strategy v0.2 |
| `pd-social-ux-research-001` (Claude designer) | 진행 → social-ux-research + user-flows v0.2 |

### Phase A — Identity & Realness (소셜의 전제)
| task | 내용 | 의존 |
|:--|:--|:--|
| `be-user-identity-001` | User/Profile 엔티티 + **Google OAuth 로그인**(Incremental Auth로 YouTube scope 예약) | po ✓ |
| `ops-spotify-live-creds-001` | 실 Spotify 자격증명 와이어링 가이드 + live 모드 점검 | po ✓ |
| `be-matching-integration-001` | MatchingEngine을 import 흐름에 통합 (**pending — 지금 실행 가능**) | 없음 |
| (A-3) Postgres 기본화 검토 | 소셜 데이터 영속화 — docker compose의 주석 블록 활성 시점 결정 | Phase C 전 |

### Phase B — YTM Corridor & 딸깍 UX
| task | 내용 | 의존 |
|:--|:--|:--|
| `be-ytm-read-adapter-001` | YouTube Data API read (1 unit/call) + videoId→Matching 정규화 | identity |
| `fe-one-click-convert-001` | **붙여넣기→목적지→변환 3-스텝** (lazy auth, 매칭실패 노출) | pd ✓ + ytm-read |
| `be-ytm-write-adapter-001` | YTM export — **쿼터 가드 + 선택적 개방 정책** 내장 | ytm-read |

### Phase C — Social MVP
| task | 내용 | 의존 |
|:--|:--|:--|
| `be-social-core-001` | Post/Like/Comment/Follow — **polyenm_pan 도메인 패턴 그대로 차용** (reaction/comment/follow 구조 + ApiResponse/예외 컨벤션 이미 도입됨) | identity |
| `fe-feed-001` | 피드 + 포스트 컴포저(자산 첨부, 500자, 무드태그) + 프로필 | social-core |
| `ranking-001` | 좋아요+저장+전환수 기반 랭킹 (실시간/주간/누적), 발견 피드형 UI | social-core |
| `gift-flow-001` | 감성 선물: 스토리+카드 커스텀 → 링크 전송 → **언박싱 연출**(곡 한 곡씩 공개) → 라이브러리 저장. BM: 프리미엄 포장 | social-core |

### Phase D — 확장 (P2)
DM · 실시간 감상방 · 외부 SNS 자동 포스팅 · 쿼터 증설 후 전면 Export 개방

---

## 3. 의존성 그래프

```
po-strategy-v2 ✓ ──┬─► be-user-identity ──┬─► be-ytm-read ──┬─► fe-one-click-convert
pd-social-ux ──────┤                      │                 └─► be-ytm-write (쿼터 가드)
                   │                      └─► be-social-core ──┬─► fe-feed
be-matching-integration (독립, 지금 가능)                      ├─► ranking
                   │                                           └─► gift-flow
                   └─► ops-spotify-live-creds (병렬)
```
병렬 창: Phase A에서 `be-user-identity` + `be-matching-integration` + `ops-spotify-live-creds` 3개 동시 가능.

## 4. KPI (PO v0.2 기준 갱신)
- **딸깍 변환 완료율**: 링크 붙여넣기 → 변환 완료 ≥ 70%
- **Import→피드 공유 전환**: 가져온 자산의 피드 게시율 ≥ 30%
- **피드 리텐션**: D7 ≥ 20% / **K-factor**(선물·공유 경유 가입) ≥ 0.4
- 매칭 신뢰도: Low Confidence 비율 < 25% (YTM 코리도)

## 5. 실행
```zsh
zsh .claude/scripts/go.sh   # pd 리서치 done 처리 후 → Phase A 3개 태스크가 pending으로
# → "Phase A 태스크 멀티에이전트로 처리해줘"
```
