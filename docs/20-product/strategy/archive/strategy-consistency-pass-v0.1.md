# [Decision Memo] 전략적 일관성 검토 및 MVP 방향 확정 v0.1

## 1. Executive Decision Summary (의사결정 요약)
본 메모는 `prd-v0.2`, `product-strategy-v0.1`, `mvp-feature-priority-v0.1` 간의 상충 요소를 제거하고, 팀의 리소스를 **"비대칭 플랫폼 브릿지(Spotify to Apple Music)"**와 **"정서적 자산화"**에 집중시키기 위해 작성되었습니다.

**3대 핵심 결정:**
1. **B2B 포지셔닝:** MVP에서는 **'완전 제외(Future Expansion)'**합니다. 데이터 스키마 수준의 제약으로만 유지하며, 문제 정의 및 요구사항에서 삭제합니다.
2. **인간 증명(Human Authenticity):** P0 출시 버전에서는 **'기능적 증명'을 배제**하고, '사용자의 정성적 기록(일기, 사진)'을 통한 **'정서적 증명'**에만 집중합니다. 센서 기반 증명은 P1(출시 후)으로 이동합니다.
3. **출시 범위(Release Scope):** **'Option A(Spotify 가져오기 + Apple Music 내보내기)'**를 냉동 MVP 범위로 확정합니다.

---

## 2. Conflict Resolution & Required Edits

### Conflict 1: B2B/기업용 데이터 판매의 시점
- **현상:** PRD에서는 문제 정의(2절)와 비즈니스 모델(11절)에 포함되어 있으나, 전략 문서에서는 'Distraction'으로 분류됨.
- **결정:** **미래 확장(Future Expansion)**. MVP 문서의 핵심 독자인 엔지니어와 디자이너에게 혼선을 줄 수 있으므로, 로드맵을 제외한 모든 섹션에서 삭제합니다.
- **문서 수정 사항:**
    - `prd-v0.2.md`: 2절(문제 정의), 4절(타겟 사용자), 11절(비즈니스 모델)에서 B2B 관련 서술 삭제.
    - `product-strategy-v0.1.md`: 4절(전략적 상충 해결)에서 B2B를 'MVP 배제'로 명문화.

### Conflict 2: 인간성 증명(Proof-of-Human)의 깊이
- **현상:** PRD에서는 '핵심 기능'으로 정의되어 있으나, 전략 문서에서는 P1(Should have)으로 밀려 있음.
- **결정:** **P1(출시 후 강화 항목)**. MVP P0의 본질은 '기계적 증명'이 아니라 '정서적 자산화'입니다. 센서 데이터 수집 및 판정 엔진은 P1으로 이동시키고, P0에서는 사용자가 직접 남기는 '맥락(일기, 사진)'의 보존에만 집중합니다.
- **문서 수정 사항:**
    - `prd-v0.2.md`: 7절(주요 기능)에서 Proof-of-Human을 P1으로 명기. 10절(데이터 구조)에서 '인간성 메타데이터'를 '선택적(Optional)'으로 변경.
    - `mvp-feature-priority-v0.1.md`: Tier 1(P0)에서 'B2B 후보 동의 저장' 삭제, Tier 2(P1)로 '기본 proof signal' 이동.

### Conflict 3: 초기 플랫폼 연동 범위 (Import/Export)
- **현상:** 모든 문서가 Spotify/Apple Music 양방향 연동을 암시하고 있어 구현 및 QA 범위가 비대해짐.
- **결정:** **Option A (Spotify Import + Apple Music Export)**.
    - **사유:** '플랫폼 이식성'이라는 제품 가설을 가장 저비용으로 검증할 수 있는 모델입니다. 가장 큰 플레이리스트 생태계(Spotify)에서 자산을 추출하여, 새로운 대안(Apple Music)으로 옮기는 'Migrator' 시나리오에 집중합니다. 양방향 지원은 P1으로 미룹니다.
- **문서 수정 사항:**
    - `prd-v0.2.md`: 6절(MVP 범위)을 'Spotify 가져오기 + Apple Music 내보내기'로 한정.
    - `product-strategy-v0.1.md`: 6절(MVP Boundary) P0 범위를 Option A로 명시.
    - `mvp-feature-priority-v0.1.md`: Tier 1 및 Must 리스트를 Option A 기준으로 축소.

---

## 3. Frozen MVP Release Definition (냉동 MVP 범위)

**"Spotify의 추억을 Apple Music의 자산으로"**

1. **가져오기(Import):** Spotify 플레이리스트 URL 기반 가져오기 (OAuth는 선택 사항).
2. **정규화(Normalize):** ISRC 기반으로 곡을 식별하여 플랫폼 독립적 원장 생성.
3. **자산화(Assetize):** 커버 이미지 업로드, 한 줄 일기(메모), 감정 태그 첨부.
4. **보관 및 공유(Save & Share):** 로그인 없이도 볼 수 있는 웹 감상 페이지 제공.
5. **내보내기(Export):** 생성된 PA를 Apple Music 플레이리스트로 전송.

---

## 4. Explicit Non-MVP List (MVP 제외 항목)
- **Apple Music에서의 가져오기 / Spotify로의 내보내기** (P1)
- **YouTube Music 관련 모든 기능** (P2)
- **앱 내부 음악 재생 엔진** (외부 앱 연결로 대체)
- **선물하기 및 실제 결제/정산 기능** (P2)
- **센서 기반 자동 인간 증명 및 점수화** (P1)
- **B2B 데이터 판매 및 기업용 콘솔** (Future)

---

## 5. Next Step Recommendation (후속 작업 제안)
1. **문서 동기화:** 위 결정 사항에 따라 3개 전략 문서의 surgical edit 즉시 수행.
2. **디자인 착수:** 'Spotify -> Apple Music' 전환 흐름과 '자산 상세(웹)' 디자인에 집중.
3. **기술 설계:** Spotify API(Read)와 Apple Music API(Write)에 특화된 Adapter 우선 구현.

---
**Product Owner**, 2026-04-11
