# [PRD] plshare2 v0.2

> **Status: SUPERSEDED.** 현재 제품 범위는
> `docs/20-product/strategy/product-baseline-v2.md`를 따른다.
> 이 문서는 Spotify -> Apple 시기의 역사적 PRD로 보존한다.

## Related Documents
- `docs/20-product/requirements/playlist-first-product-intent.md`
- `docs/20-product/requirements/functional-requirements-v0.2.md`
- `docs/20-product/requirements/non-functional-requirements-v0.2.md`
- `docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `docs/20-product/research/market-research-v0.3.md` (Latest Insight)

## 1. 제품 요약 (Product Summary) - Repositioned
**"음악을 재생하는 서비스가 아니라, 플랫폼을 넘어 이동하고 보존되며 선물될 수 있는 `취향 자산`을 만드는 서비스"**

사용자의 플레이리스트와 감성 기록(일기, 사진, 맥락)을 특정 스트리밍 플랫폼에 종속되지 않는 독립적인 '자산(PA - Playlist Asset)'으로 저장하고, 어느 플랫폼에서든 즉시 재생 가능하게 연결해 주는 '취향 자산 매니지먼트 레이어'입니다.

## 2. 문제 정의 (Problem Statement)
*   **플랫폼 종속성:** 사용자의 정성 어린 플레이리스트와 기록이 Spotify나 Apple Music 같은 플랫폼 내에 갇혀 있으며, 플랫폼 이동 시 자산이 손실됨.
*   **맥락의 휘발 (Context Loss):** 현재의 플레이리스트는 곡의 나열일 뿐이며, "왜 이 곡들을 골랐는지"에 대한 인간적 서사는 보존되지 않음.
*   **음원 저작권 장벽:** 직접 음원을 서비스하려 할 경우 막대한 라이선스 비용과 플랫폼 정책 리스크에 직면함.

## 3. 비전 및 원칙 (Vision and Principles)
*   **비전:** "인간의 감성 맥락을 플랫폼 비종속적 자산으로 전환하여 데이터 주권을 회복한다."
*   **원칙:**
    1.  **Player-Agnostic:** 특정 재생 엔진에 종속되지 않는 메타데이터 중심 구조를 지향한다.
    2.  **Emotional Asset:** 단순 목록이 아닌 '자산'처럼 느껴지는 UX를 지향한다.
    3.  **Data Portability:** "내 취향은 어디든 옮길 수 있다"는 가치를 최우선으로 한다.

## 4. 타겟 사용자 (Target Users)
*   **Primary (데이터 생산자):** 자신의 취향을 기록하고 플랫폼 이동에 민감한 헤비 리스너 및 MZ세대 크리에이터.
*   **Secondary (감성 기록가):** 자신의 음악적 추억을 단순히 듣는 것에 그치지 않고 자산으로 간직하고 싶은 사용자.

## 5. 핵심 사용자 시나리오 (Core User Scenarios) - Updated
1.  **가져오기(Import):** Spotify의 플레이리스트를 URL 또는 OAuth로 들여옴.
2.  **정규화 및 기록(Normalize & Log):** 곡들을 ISRC 기반으로 정규화하고, 사용자가 커버, 일기, 메모, 감정 태그를 붙여 독립된 'PA'로 저장함.
3.  **자산화(Assetization):** 기록은 사용자가 직접 남긴 일기, 사진, 감정 태그와 함께 정서적 자산으로 보관됨.
4.  **내보내기 및 공유(Export & Share):** 저장된 PA를 Apple Music으로 내보내거나, 감상 페이지 링크로 공유함.

## 6. MVP 범위 (MVP Scope) - Strategic Update
*   **핵심 목표:** `Spotify -> Apple Music` 비대칭 브릿지와 감성 자산화 경험을 검증한다.
*   **범위:**
    *   **Normalization Engine:** ISRC 및 플랫폼 ID 매핑을 통한 곡 식별 엔진.
    *   **Emotional Logger:** 텍스트 일기, 사진, 감정 태그 입력 기능.
    *   **Platform Adapters:** Spotify 가져오기(Import) 및 Apple Music 내보내기(Export) 집중.
    *   **Consumption Layer:** 앱 내부 재생 대신 '감상 페이지' 및 '외부 앱에서 열기' 중심 UX.

## 7. 주요 기능 (Core Features)
*   **Metadata Ledger:** 플랫폼 독립적인 곡 식별자 및 맥락 데이터 저장소.
*   **Asymmetric Bridge:** Spotify에서 가져와 Apple Music으로 보내는 이동 도구.
*   **Contextual Proof (P0):** 사용자가 직접 남긴 일기와 사진을 통한 정서적 진본성 확보.
*   **Proof-of-Human Signal (P1):** 기기 센서 및 작성 패턴 기반의 기술적 검증 레이어.
*   **Asset Link:** 수신자의 플랫폼에 맞춰 최적의 재생 환경으로 연결하는 공유 링크.

## 8. 기능 요구사항 (Functional Requirements)
*   **정규화:** 수집된 곡 정보를 ISRC 기반으로 변환하여 저장해야 함.
*   **내보내기:** Apple Music API를 사용하여 플레이리스트 생성 및 곡 추가 기능을 제공해야 함.
*   **기록 관리:** PA 생성 시 작성된 텍스트와 사진을 메타데이터와 결합하여 암호화 저장.

## 9. 비기능 요구사항 (Non-functional Requirements)
*   **정책 준수:** 각 플랫폼(Spotify 2026 등)의 최신 개발자 정책을 엄격히 준수(상업적 재생 금지 등).
*   **매칭 정확도:** 플랫폼 간 곡 매칭 실패 시 수동 매칭 또는 대체 트랙 추천 프로세스 필요.
*   **확장성:** 향후 다양한 스트리밍 서비스(Tidal, Deezer 등)를 지원할 수 있는 Adapter 패턴 적용.

## 10. 데이터 구조 (Data Structure)
*   **Canonical Playlist Asset:** [제목 / 설명 / 커버 / 일기 / 감정 태그 / 선택적 인간성 메타데이터(P1)]
*   **Canonical Track Identity:** [ISRC / Spotify ID / Apple Music ID / YouTube videoId / 제목 / 아티스트 / 매칭 신뢰도]

## 11. 비즈니스 모델 및 수익화 가설
*   **BM:** 프리미엄 내보내기 기능, PA 선물하기 수수료.

## 12. 리스크 및 미결 사항 (Risks and Open Questions) - Updated
*   **[리스크] 플랫폼 차단:** Spotify 등 대형 플랫폼이 서드파티의 데이터 이동 기능을 기술적으로 차단하거나 정책적으로 금지할 가능성.
*   **[리스크] 정규화 한계:** ISRC가 누락된 독립 음원이나 지역 제한 곡의 매칭 난이도.
*   **[미결]** YouTube Music의 `videoId` 중심 구조를 `track` 기반 정규화 엔진에 어떻게 안정적으로 통합할 것인가?

## 13. 성공 지표 (Success Metrics)
*   **Import/Export Ratio:** 유입된 사용자가 실제 자산을 생성하고 다른 플랫폼으로 내보내는 비율.
*   **Context Density:** 생성된 PA 중 텍스트/사진 등 인간 맥락 데이터가 포함된 비중.
*   **Share Viral K-factor:** 공유된 PA 링크를 통해 신규 유입되는 사용자의 수.

## 14. 향후 계획 (Roadmap)
*   **v0.3:** Apple Music 가져오기, Spotify로 내보내기, 수동 매칭 보정 등 양방향 브릿지 강화.
*   **v0.4:** 기본 proof signal 수집 및 소셜 기능 강화.
*   **Future Expansion:** B2B 데이터 패키징, 고도화된 proof, 영지식 증명(ZKP) 검토.
