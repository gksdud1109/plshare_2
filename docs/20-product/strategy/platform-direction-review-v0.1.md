# Platform Direction Review v0.1

> **Status: SUPERSEDED.** 2026-06-10의
> `platform-strategy-v2-research-v0.1.md` 결정이 이 권고를 대체한다.

## 문서 정보
- Date: `2026-04-14`
- Role: `Product Owner`
- Model: `Gemini`
- Related Task: `po-platform-direction-review-001`

---

## 1. Executive Summary
이 문서는 현재 MVP 방향인 `Spotify -> Apple Music` 비대칭 브릿지를 유지할지, 아니면 `YouTube Music 중심 export` 전략으로 전환할지를 검토한다.

최종 권고는 명확하다.

**현재 방향을 유지한다.**

즉:
- MVP는 `Spotify Import -> Add Emotional Context -> Apple Music Export`
- `YouTube Music`은 `P2 확장`으로 유지

이유는 단순하다.
- `Emotional Context` 자산화 방향과 Apple 쪽의 브랜드/사용자 기대가 더 잘 맞는다
- `YouTube Music`의 `videoId` 중심 구조는 정규화 무결성을 해친다
- MVP에서 검증해야 할 것은 `무료 대량 유입`보다 `감성 자산화 + 이동성`이다

---

## 2. Strategic Context
- Current Direction: `Spotify Import -> Add Emotional Context -> Apple Music Export`
- Core Value: `Emotional Context`가 단순 플레이리스트를 `Playlist Asset (PA)`로 전환한다
- Approved Naming: 인간이 직접 남긴 맥락 레이어의 공식 명칭은 `Emotional Context`다

---

## 3. Comparative Analysis

| Factor | Apple Music (Current) | YouTube Music (Proposed) | Assessment |
| :--- | :--- | :--- | :--- |
| Product Value | 프리미엄, 아카이브/자산 감각과 잘 맞음 | 유틸리티 중심, 자산 감각이 약해질 위험 | Apple 우세 |
| Normalization | Track/ISRC 기반, 매칭 안정성 높음 | `videoId` 중심, 비공식 업로드/버전 혼선 위험 | Apple 우세 |
| Policy Risk | 상대적으로 예측 가능하고 developer-friendly | quota, 정책, audio/video 경계 이슈 큼 | Apple 우세 |
| UX/Reach | 고품질, 유료 사용자층 중심 | 도달 범위는 넓지만 컨텍스트 노출 UX가 산만함 | YouTube 우세 |

정리하면:
- `도달 범위`만 보면 YouTube Music이 유리
- 하지만 현재 제품의 핵심 가설과 데이터 무결성은 Apple 쪽이 더 잘 지킨다

---

## 4. Evaluation Of The YouTube-Centered Routes

### Apple Music -> YouTube Music
- Apple이 제공하는 공식 전송 경험과 비교해 wedge 차별성이 약하다
- 우리의 추가 가치는 `Emotional Context` 외에는 제한적이다

### Spotify -> YouTube Music
- 사용자 규모는 클 수 있다
- 하지만 `무료 사용자` 유입 비중이 높아질수록 자산화, 저장, 프리미엄 전환 가설이 약해질 수 있다
- `videoId` 매칭 품질 문제가 곧 제품 신뢰도 문제로 이어질 수 있다

---

## 5. Decision Rationale

### 1. Data Integrity
우리 제품의 핵심은 `정규화된 Ledger`다.

YouTube Music은 `videoId` 중심이라:
- live
- radio edit
- fan upload
- region-specific variant

같은 혼선이 쉽게 발생한다.

이건 `자산`의 무결성을 해친다.

### 2. Brand Fit
우리는 `무료 음악 우회 도구`가 아니라 `취향 자산 레이어`를 만든다.

Apple Music은:
- 큐레이션 감각
- 프리미엄 라이브러리 이미지
- 보관/아카이브 맥락

이 강해서 `Emotional Context`와 더 잘 맞는다.

### 3. MVP Focus
지금 검증해야 하는 건:
- 가져오기
- 감성 맥락 부착
- 자산 저장
- 타 플랫폼 내보내기

이 네 단계다.

`YouTube Music`은 이 루프를 빠르게 검증하게 해주기보다, 정규화와 정책 리스크를 늘린다.

---

## 6. Final Recommendation

**Decision: Maintain the Asymmetric Apple Strategy.**

### Phase 0. MVP
- `Spotify -> Apple Music` 루프를 안정화한다
- `Emotional Context` 입력 UX에 집중한다

### Phase 1
- Apple Music import를 추가해 premium bidirectional bridge를 강화한다

### Phase 2
- YouTube Music을 secondary export adapter로 검토한다
- 이 단계에서는 `videoId` 매핑 신뢰도와 정규화 엔진 성숙도가 전제돼야 한다

---

## 7. Operational Impact
- `pd-ux-flow-001`은 Apple export 기준으로 진행한다
- `be-adapter-arch-001`은 Apple write adapter 기준 문서를 먼저 작성한다
- `fe-implementation-plan-001`은 Apple export UX를 전제로 route/state를 설계한다
- `YouTube Music`은 현재 구현 범위에서 제외한다

---

## 8. Freeze
- MVP 방향: `Spotify Import -> Add Emotional Context -> Apple Music Export`
- Approved naming: `Emotional Context`
- YouTube Music status: `P2 / Future Expansion`
