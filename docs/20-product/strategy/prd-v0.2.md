# [PRD] Life-Logging Ledger (가칭: PA - Playlist Asset) v0.2

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
*   **데이터의 저질화(Model Collapse):** AI 생성 데이터가 범람하며 '진짜 인간'이 직접 큐레이션하고 맥락을 담은 데이터의 가치가 급등함.
*   **음원 저작권 장벽:** 직접 음원을 서비스하려 할 경우 막대한 라이선스 비용과 플랫폼 정책 리스크(특히 YouTube/Spotify)에 직면함.

## 3. 비전 및 원칙 (Vision and Principles)
*   **비전:** "인간의 감성 맥락을 플랫폼 비종속적 자산으로 전환하여 데이터 주권을 회복한다."
*   **원칙:**
    1.  **Player-Agnostic:** 특정 재생 엔진에 종속되지 않는 메타데이터 중심 구조를 지향한다.
    2.  **Human-Authenticity:** AI가 흉내 낼 수 없는 인간의 물리적 맥락(일기, 사진, 작성 패턴)을 증명하고 연결한다.
    3.  **Data Portability:** "내 취향은 어디든 옮길 수 있다"는 가치를 최우선으로 한다.

## 4. 타겟 사용자 (Target Users)
*   **Primary (데이터 생산자):** 자신의 취향을 기록하고 플랫폼 이동에 민감한 헤비 리스너 및 MZ세대 크리에이터.
*   **Secondary (데이터 소비자):** '인간의 맥락이 포함된 정제된 취향 데이터'를 필요로 하는 AI 테크 기업 및 마케팅 기업.

## 5. 핵심 사용자 시나리오 (Core User Scenarios) - Updated
1.  **가져오기(Import):** Spotify/Apple Music의 플레이리스트를 URL 또는 OAuth로 들여옴.
2.  **정규화 및 기록(Normalize & Log):** 곡들을 ISRC 기반으로 정규화하고, 사용자가 커버, 일기, 메모, 감정 태그를 붙여 독립된 'PA'로 저장함.
3.  **자산화(Assetization):** 검증된 기록은 '인간 생성 증명'과 함께 자산화되어 보관됨.
4.  **내보내기 및 공유(Export & Share):** 저장된 PA를 다시 다른 스트리밍 서비스(예: Apple Music으로 가져온 걸 Spotify로)로 내보내거나, 감상 페이지 링크로 공유함.

## 6. MVP 범위 (MVP Scope) - Strategic Update
*   **핵심 목표:** `Playlist Asset Ledger` 구조의 기술적 검증 및 `Import/Export` 성장 루프 구현.
*   **범위:**
    *   **Normalization Engine:** ISRC 및 플랫폼 ID 매핑을 통한 곡 식별 엔진.
    *   **Emotional Logger:** 텍스트 일기, 사진, 감정 태그 입력 기능.
    *   **Platform Adapters:** Spotify 및 Apple Music 연동 (가져오기/내보내기). YouTube는 후순위.
    *   **Consumption Layer:** 앱 내부 재생 대신 '감상 페이지' 및 '외부 앱에서 열기' 중심 UX.

## 7. 주요 기능 (Core Features)
*   **Metadata Ledger:** 플랫폼 독립적인 곡 식별자 및 맥락 데이터 저장소.
*   **Cross-platform Bridge:** 플랫폼 간 플레이리스트 이동 및 동기화 도구.
*   **Proof-of-Human Context:** 기기 센서 및 작성 패턴 기반의 인간성 검증 레이어.
*   **Asset Link:** 수신자의 플랫폼에 맞춰 최적의 재생 환경으로 연결하는 공유 링크.

## 8. 기능 요구사항 (Functional Requirements)
*   **정규화:** 수집된 곡 정보를 ISRC 기반으로 변환하여 저장해야 함.
*   **내보내기:** 타겟 플랫폼의 API를 사용하여 플레이리스트 생성 및 곡 추가 기능을 제공해야 함.
*   **기록 관리:** PA 생성 시 작성된 텍스트와 사진을 메타데이터와 결합하여 암호화 저장.

## 9. 비기능 요구사항 (Non-functional Requirements)
*   **정책 준수:** 각 플랫폼(Spotify 2026 등)의 최신 개발자 정책을 엄격히 준수(상업적 재생 금지 등).
*   **매칭 정확도:** 플랫폼 간 곡 매칭 실패 시 수동 매칭 또는 대체 트랙 추천 프로세스 필요.
*   **확장성:** 향후 다양한 스트리밍 서비스(Tidal, Deezer 등)를 지원할 수 있는 Adapter 패턴 적용.

## 10. 데이터 구조 (Data Structure)
*   **Canonical Playlist Asset:** [제목 / 설명 / 커버 / 일기 / 감정 태그 / 인간성 메타데이터]
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
*   **v0.3:** 소셜 기능 및 PA 선물하기 기능 강화.
*   **v0.4:** B2B 데이터 마켓플레이스 알파 테스트 및 음원 제외한 '인간 취향 구조 데이터' 상품화.
*   **v0.5:** 영지식 증명(ZKP) 기반 프라이버시 보호 기술 도입.
