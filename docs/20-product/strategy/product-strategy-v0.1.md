# [Product Strategy] plshare2 v0.1

## 1. Executive Summary: The Sharp Angle
본 제품은 **"음악을 재생하는 도구"**가 아니라, **"플랫폼에 갇힌 인간의 취향을 해방하고 자산화하는 레이어"**다. 

우리는 Spotify나 Apple Music과 경쟁하지 않는다. 대신, 그들 사이를 흐르는 **'맥락이 담긴 데이터(Contextual Data)'**의 주권을 사용자에게 돌려줌으로써, AI 시대에 가장 귀한 '인간 생성 진본 데이터'의 저장소가 된다.

---

## 2. Core Problem Definition (진짜 해결하려는 문제)
1.  **플랫폼 인질 (Platform Lock-in):** 사용자가 수년간 쌓아온 플레이리스트와 그 안의 추억은 스트리밍 서비스의 DB에 갇혀 있어, 플랫폼 이동 시 증발하거나 파편화된다.
2.  **맥락의 소실 (Context Loss):** 현재의 플레이리스트는 '곡 ID의 나열'일 뿐이다. "왜 이 곡들을 골랐는지", "당시 어떤 기분이었는지"에 대한 인간적 맥락은 어디에도 기록되지 않는다.
3.  **데이터 가치 소외 (Data Alienation):** AI 모델들은 인간의 취향 데이터를 학습하여 막대한 부를 쌓지만, 정작 데이터를 생산한 개인은 아무런 보상을 받지 못한다.

---

## 3. Product Thesis (제품 가설 및 명제)
> **"맥락이 없는 취향은 휘발되는 데이터지만, 인간의 서사가 결합된 취향은 복제 불가능한 자산(Asset)이 된다."**

- **Wedge (유입책):** "내 플레이리스트를 어디든 옮길 수 있다"는 실용적 편리함.
- **Value (본질적 가치):** "내 음악 취향과 그날의 감정을 영구히 소유하고 선물할 수 있다"는 정서적 만족.
- **Moat (방어기제):** 플랫폼이 흉내 낼 수 없는 '인간성 증명 메타데이터'와 그들 사이를 잇는 '정규화된 원장(Ledger)'.

---

## 4. Strategic Tension Resolution (전략적 상충 해결)

현재 문서들 사이에 존재하는 전략적 긴장 요소를 아래와 같이 강제로 정리한다.

| 요소 | 전략적 지위 | 결정 (Decision) |
| :--- | :--- | :--- |
| **Playlist as Emotional Asset** | **Primary Value** | **제품의 영혼.** 모든 기능은 '단순 목록'이 아닌 '소중한 자산'처럼 느껴지게 설계한다. |
| **Cross-platform Import/Export** | **Supporting Mechanism** | **강력한 Wedge.** 초기 유입을 만드는 '도구적 가치'다. 이것 없이는 자산화 단계로 진입하지 않는다. |
| **Player-Agnostic Ledger** | **Foundational Tech** | **기술적 실체.** 플랫폼에 종속되지 않는 정규화된 데이터 구조를 통해 자산의 영속성을 보장한다. |
| **Future B2B Data Sales** | **Future Expansion** | **MVP 단계의 Distraction.** 데이터 구조는 열어두되, 현재 구현 및 마케팅에서는 완전히 배제한다. |

---

## 5. Value Proposition Hierarchy (가치 제안 계층)
1.  **[Bottom - Utility]:** 플랫폼 간 플레이리스트 이동 및 보존 (이동성).
2.  **[Middle - Emotional]:** 내 음악 취향에 일기, 사진, 감정을 담아 기록 (자산화).
3.  **[Top - Social/Comm.]:** 소중한 사람에게 내 취향 자산을 선물하고 공유 (연결).

---

## 6. MVP Boundary & Priority Framework (P0/P1/P2)

모든 것을 중요하다고 하지 않는다. **'Emotional Portability'** 루프 완성에만 집중한다.

### P0: 핵심 루프 (Must have for Launch)
- **Import:** Spotify/Apple Music URL을 넣으면 곡 목록을 긁어옴.
- **Normalize:** ISRC 기반으로 곡을 식별하여 우리만의 'Ledger'에 저장.
- **Personalize:** 커버 이미지, 한 줄 일기, 감정 태그를 붙여 'PA'로 전환.
- **Export:** 저장된 PA를 다시 다른 플랫폼(예: Spotify -> Apple Music)으로 내보내기.
- **Share:** 로그인 없이도 볼 수 있는 아름다운 '감상 페이지' 링크 생성.

### P1: 가치 강화 (Should have shortly after)
- **Humanity Proof (Basic):** 작성 시간, 편집 패턴 등 기초적인 인간 생성 신호 수집.
- **Social Interaction:** 타인의 PA '저장하기', '좋아요', 작성자 팔로우.
- **Manual Match:** 정규화 실패 시 사용자가 직접 곡을 검색해서 매칭하는 UX.

### P2: 확장 및 수익화 (Deferred Bets)
- **Gifting/Commerce:** 포인트/토큰 기반의 PA 구매 및 선물하기 UX.
- **Advanced Proof:** 센서 데이터, GPS 등을 활용한 고도화된 진본성 검증.
- **YouTube Adapter:** 기술적/정책적 난도가 높은 YouTube Music 완전 지원.
- **B2B Console:** 기업용 데이터 패키징 및 판매 인터페이스.

---

## 7. Target User Priority (우선순위 타겟)
1.  **Primary:** **"The Platform Migrator"** (스트리밍 서비스를 갈아타려 하지만 플레이리스트 때문에 망설이는 사용자).
2.  **Secondary:** **"The Emotional Archivist"** (자신의 음악 취향을 단순히 듣는 것에 그치지 않고 기록하고 간직하고 싶은 사용자).
3.  **Distraction:** "단순히 공짜로 음악을 듣고 싶은 사용자" (우리는 재생기가 아니다).

---

## 8. Market & Hypothesis Alignment (가설 및 검증 계획)

| 핵심 가설 (Hypothesis) | 검증 방법 (Validation) | 성공 지표 (Success Metric) |
| :--- | :--- | :--- |
| 사용자는 플랫폼 이동성(Wedge)을 위해 기꺼이 계정을 연결할 것이다. | MVP Import 사용성 테스트 | 연결 시도 대비 완료율 > 70% |
| 사용자는 단순 목록보다 맥락(일기/사진)이 붙은 PA에 더 애착을 느낀다. | Context 입력 필드 활성화율 측정 | PA 생성 중 텍스트/이미지 추가율 > 40% |
| '선물/구매' 이전에 '공유된 링크'를 통해 유입되는 경험이 유효하다. | 공유 링크를 통한 신규 가입 트래킹 | Viral K-factor > 0.2 |

---

## 9. Key Risks & Mitigation (리스크 대응)
1.  **Platform Policy Risk:** Spotify/Apple이 서드파티의 내보내기를 차단할 수 있음.
    - *Mitigation:* 특정 플랫폼 의존도를 낮추고, '감상 페이지(Web)' 자체의 가치를 높여 독자적 생태계 구축.
2.  **Normalization Quality:** 곡 매칭 실패 시 사용자 경험 저하.
    - *Mitigation:* 매칭 신뢰도를 투명하게 공개하고, 사용자가 직접 수정할 수 있는 도구 제공.
3.  **Data Privacy:** 일기, 사진 등 민감 정보 수집에 대한 거부감.
    - *Mitigation:* B2B 활용 동의를 명확히 분리하고, 초기에는 기기 내 저장/암호화 강조.

---

## 10. Decision Rules (의사결정 원칙)
- **"재생 기능을 넣을까?"** -> 아니오. 외부 앱(Open in Spotify/Apple)으로 보낸다. 우리 앱은 '기록과 연결'에 집중한다.
- **"YouTube Music부터 할까?"** -> 아니오. 메타데이터가 정제된 Spotify/Apple Music부터 완성한다.
- **"B2B를 위해 센서 데이터를 다 긁을까?"** -> 아니오. 사용자 경험을 해치지 않는 선에서 '동의' 기반으로만 시작한다.

---

## 11. Next Handoffs (후속 작업 지침)
- **Product Designer:** `docs/20-product/design/` 하위에 '자산'으로서의 가치가 느껴지는 카드/상세 페이지 UI 설계 (단순 리스트 지양). `ux-spec-authoring` 스킬 활용 권장.
- **Engineer:** `docs/20-product/requirements/` 하위 기능/비기능 요구사항을 바탕으로 ISRC 기반의 정규화 엔진 및 플랫폼별 Adapter 패턴 아키텍처 설계.
- **Operations:** `docs/20-product/research/` 기반으로 "취향을 자산으로, 추억을 이동가능하게"라는 메시지로 온보딩 캠페인 준비.
