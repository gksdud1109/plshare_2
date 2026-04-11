# Gemini Prompt for Market Research v0.2

아래 프롬프트를 그대로 Gemini에 붙여넣으면 된다.

```text
You are working as the `Gemini Strategist` for this project.

Your job is to deepen the market research for the playlist-first product strategy.

Read the following project documents and produce a more detailed market research document:
- Input source 1: `docs/20-product/strategy/archive/prd-v0.1.md`
- Input source 2: `docs/20-product/requirements/playlist-first-product-intent.md`
- Output target: `docs/20-product/research/archive/market-research-v0.2.md`

Important operating rules:
1. Focus on the real market meaning of a playlist-first product.
2. Use the clarified product intent below as the primary lens:
   - users create their own playlists
   - playlists should be credibly human-generated
   - users can rank, buy, and gift playlists
   - the UX should make playlists feel emotionally valuable as gifts or purchases
   - playlist data plus cover images, diaries, notes, letters, and metadata can later be sold to enterprises as AI training data
3. Separate consumer market analysis and enterprise market analysis.
4. Distinguish direct competitors, adjacent competitors, and enabling infrastructure.
5. Identify what should be validated in MVP versus what can remain a strategic hypothesis.
6. Be skeptical. Do not present assumptions as facts.
7. Write in Korean.

Do not do these:
- Do not write code.
- Do not redesign the whole PRD.
- Do not finalize pricing.
- Do not assume legal or policy issues are solved.
- Do not over-focus on token economics.

The output should include:
- Research objective
- Key market hypotheses
- Consumer behavior signals
- Competitor landscape
- Adjacent market analogies
- Enterprise demand signals for verified human data
- Risks and barriers
- Product positioning implications
- Recommended validation order for MVP
- Open questions

Please make the output practical for product planning.
For each major conclusion, clearly distinguish:
- observed market signal
- interpretation
- implication for our product

Use the project context below.

----- BEGIN PROJECT CONTEXT -----
[PRD v0.1]
# [PRD] plshare2 v0.1

## 1. 제품 요약 (Product Summary)
AI가 생성한 데이터가 범람하는 시대에, '인간만이 생성할 수 있는 진본 데이터(Human-Authenticity)'를 수집, 검증 및 자산화하는 플랫폼입니다. 개인의 음악 취향, 감정 기록, 생활 맥락을 결합하여 고유한 가치를 지닌 '플레이리스트 에셋(PA)'을 생성하고 이를 통해 데이터 주권을 회복합니다.

## 2. 문제 정의 (Problem Statement)
*   **데이터의 저질화(Model Collapse):** AI 생성 데이터가 인터넷을 점유하면서 AI 모델 학습을 위한 '진짜 인간'의 데이터 희소성이 급증함.
*   **데이터 주권 부재:** 개인은 매일 고유한 감성 데이터를 생산하지만, 그 이익은 거대 플랫폼 기업이 독점함.
*   **신뢰의 위기:** 디지털 환경에서 무엇이 인간의 창작물이고 무엇이 AI의 결과물인지 구분하기 어려워짐.

## 3. 비전 및 원칙 (Vision and Principles)
*   **비전:** "존재하고 느끼는 것만으로도 가치가 되는 인간 중심 경제 구축"
*   **원칙:**
    1.  **진본성 우선:** AI 봇이 흉내 낼 수 없는 인간의 물리적, 생체적 맥락을 증명한다.
    2.  **사용자 주권:** 데이터의 소유권과 수익권은 생산한 개인에게 귀속된다.
    3.  **심리스한 경험:** 블록체인 기술은 배경에서 작동하며, 사용자는 일반적인 앱 사용 경험을 유지한다.

## 4. 타겟 사용자 (Target Users)
*   **Primary (데이터 생산자):** 자신의 취향과 일상을 기록하고 공유하는 것을 즐기는 MZ세대 및 크리에이터.
*   **Secondary (데이터 소비자):** 양질의 인간 학습 데이터를 필요로 하는 AI 테크 기업, 초개인화 서비스를 지향하는 스트리밍 플랫폼.

## 5. 핵심 사용자 시나리오 (Core User Scenarios)
1.  **기록 및 큐레이션:** 사용자가 특정 감정이나 상황(예: 비 오는 날의 산책)에 맞는 음악을 선택하고, 당시의 사진과 짧은 일기를 작성함.
2.  **인간 증명:** 앱은 촬영 당시의 기기 센서 데이터와 작성 패턴을 분석하여 '인간의 실제 경험'임을 백그라운드에서 검증함.
3.  **자산화:** 검증된 기록은 'PA(Playlist Asset)'라는 단위로 저장되며, 추후 거래 가능한 형태의 증서로 변환됨.
4.  **수익 창출:** 자신의 PA가 AI 학습 데이터로 채택되거나 타인에게 공유될 때 보상(포인트/토큰)을 획득함.

## 6. MVP 범위 (MVP Scope)
*   **핵심 목표:** 데이터 수집 루프 완성 및 기초적인 '인간 증명' 메커니즘 검증.
*   **범위:**
    *   음악 검색 및 플레이리스트 생성 기능 (외부 API 연동).
    *   멀티모달 기록(텍스트 일기, 사진 업로드) 기능.
    *   기초적인 기기 센서 기반 진본성 체크 (GPS, 시간대, 가속도 센서).
    *   내부 데이터베이스 기반의 자산 관리 (온체인 민팅은 선택 사항으로 유지).

[Playlist-First Product Intent]
1. 사용자가 자신만의 플레이리스트를 생성한다.
2. 이 플레이리스트가 인간이 실제로 생성한 것임을 일정 수준 증명할 수 있어야 한다.
3. 이 플레이리스트와 그 맥락 데이터가 개인 간 거래와 기업 대상 데이터 판매의 출발점이 되어야 한다.

핵심 UX 의도:
- 랭킹
- 구매
- 선물
- 감성적인 UI/UX

기업 판매 대상 데이터:
- 플레이리스트 데이터 목록
- 커버사진
- 일기
- 글
- 쪽지
- 편지
- 인간 생성 관련 메타데이터
----- END PROJECT CONTEXT -----

Return only the completed content for `docs/20-product/research/archive/market-research-v0.2.md`.
```
