# [시장 조사] Life-Logging Ledger (PA) 시장 분석 및 전략 가설 v0.3

## 1. 조사 목적 (Research Objective)
본 문서는 `market-research-v0.2.md`의 경쟁 지형 분석을 이어받아, 이 프로젝트의 가장 큰 장벽인 **음원/저작권 및 플랫폼 정책 리스크**를 제품 전략 관점에서 다시 검토한다. 특히 아래 질문에 답하는 것을 목표로 한다.

1. `YouTube Music API 연동` 중심 접근이 실제로 가장 좋은 해법인가?
2. 음원을 직접 다루지 않으면서도 사용자 경험을 해치지 않는 더 나은 구조가 있는가?
3. 외부 스트리밍 앱으로의 `들여오기/내보내기(import/export)` 자체가 유입과 성장에 의미 있는 전략이 될 수 있는가?

---

## 2. 핵심 결론 (Executive Summary)

### 결론 1. `YouTube Music 단일 연동`은 출발점으로는 이해되지만, 장기적으로는 가장 좋은 해법이 아니다.
* **관찰된 사실:** Google의 공식 개발자 문서는 `YouTube Data API`를 통해 플레이리스트 생성과 항목 추가를 지원하지만, 항목 단위는 `youtube#video`와 `videoId` 중심이다. 또한 YouTube 정책은 오디오/비디오 분리, 백그라운드 재생, YouTube 핵심 UX의 복제를 제한한다.  
  출처: [YouTube Playlists guide](https://developers.google.com/youtube/v3/guides/implementation/playlists), [playlistItems.insert](https://developers.google.com/youtube/v3/docs/playlistItems/insert), [YouTube Developer Policies](https://developers.google.com/youtube/terms/developer-policies)
* **해석:** YouTube는 "음악 카탈로그를 중립적으로 다루는 데이터 레이어"라기보다 "동영상 플레이리스트 레이어"에 가깝다. 따라서 우리 서비스가 음악 자산의 정규화 계층이 되기에는 구조적 불일치가 있다.
* **제품 시사점:** `YouTube/YouTube Music`은 지원 대상 중 하나일 수는 있어도, 전체 제품 구조를 그 위에 얹는 기본 축으로 삼는 것은 위험하다.

### 결론 2. 가장 현실적인 해법은 `Player-Agnostic Metadata Ledger`다.
* **관찰된 사실:** Spotify는 사용자 개인 데이터 또는 사용자의 플레이리스트 메타데이터를 다른 서비스로 이전하는 목적은 허용한다고 명시한다. Apple은 2025년 3월 25일과 2025년 4월 18일 문서에서 Apple Music <-> YouTube Music 간 플레이리스트 이전 시 `음악 파일은 이전되지 않는다`고 명확히 설명한다. 또한 2026년 4월 현재 Apple Support 문서는 타 음악 서비스의 음악/플레이리스트를 Apple Music으로 이전하는 흐름을 공식 안내한다.  
  출처: [Spotify Developer Policy](https://developer.spotify.com/policy), [Apple: YouTube Music -> Apple Music](https://support.apple.com/en-us/107776), [Apple: Apple Music -> YouTube Music](https://support.apple.com/en-us/120030), [Apple: Transfer from other music services to Apple Music](https://support.apple.com/en-us/118249)
* **해석:** 빅테크 플랫폼들조차 실제 이전 대상은 `음악 파일`이 아니라 `플레이리스트 구조와 매칭 가능한 카탈로그 항목`으로 취급하고 있다.
* **제품 시사점:** 우리도 음원 소비를 소유하려 하지 말고, **곡 식별자 + 사용자 맥락 + 인간성 메타데이터**를 자산의 핵심으로 삼는 편이 법적/기술적으로 가장 안전하다.

### 결론 3. `플랫폼 간 플레이리스트 이동`은 유입 기능으로는 좋지만, 그것만으로는 방어력 있는 핵심 모트가 되기 어렵다.
* **관찰된 사실:** Spotify는 2026년 현재 앱 내에서 Tune My Music과 연동한 가져오기 기능을 제공하고 있고, Apple Music도 타 서비스에서의 이전 기능을 공식 지원한다.  
  출처: [Spotify Support: Importing your playlists to Spotify](https://support.spotify.com/us/article/importing-your-playlists-to-spotify/), [Apple Support: Transfer your library and playlists from music services to Apple Music](https://support.apple.com/en-us/118249)
* **해석:** 사용자는 이미 "플랫폼 간 이동"을 원하고 있으며, 이 수요는 실재한다. 그러나 이 기능은 점점 플랫폼 자체 기능으로 흡수되고 있다.
* **제품 시사점:** `import/export`는 **온보딩과 성장 루프**로 활용하되, 핵심 가치 제안은 반드시 그 위의 `감성 맥락`, `선물/구매 UX`, `인간 생성 증명`, `플랫폼 비종속 자산화`에 둬야 한다.

---

## 3. 플랫폼 현실 점검 (Platform Reality Check)

### 3.1 Spotify
* **관찰된 사실**
  * 공식 Web API로 플레이리스트 생성 및 항목 추가가 가능하다.  
    출처: [Create Playlist](https://developer.spotify.com/documentation/web-api/reference/create-playlist), [Add Items to Playlist](https://developer.spotify.com/documentation/web-api/reference/add-items-to-playlist)
  * 검색 시 `isrc` 필터를 지원한다.  
    출처: [Search for Item](https://developer.spotify.com/documentation/web-api/reference/search?q=Muse)
  * 정책상 플레이리스트 메타데이터의 타 서비스 이전은 예외적으로 허용되지만, 상업적 스트리밍 통합과 인앱 결제형 스트리밍 SDA는 제한된다. Spotify 콘텐츠를 AI 학습에 사용하는 것도 금지된다.  
    출처: [Spotify Developer Policy](https://developer.spotify.com/policy)
  * 2026년 2월 6일 Spotify는 개발자 접근 통제를 강화했고, Development Mode는 "비상업적 실험용"이며 사업의 기반으로 삼지 말라고 명시했다.  
    출처: [Spotify Developer Access Update, February 6, 2026](https://developer.spotify.com/blog/2026-02-06-update-on-developer-access-and-platform-security)
* **해석**
  * Spotify는 `metadata source`, `playlist write target`으로는 강력하다.
  * 반면, 우리 앱 내부에서 Spotify를 상업적 재생 엔진처럼 쓰는 방향은 정책 리스크가 크다.
  * B2B 데이터 사업에서도 Spotify 원본 콘텐츠 의존도를 낮춰야 한다.
* **제품 시사점**
  * Spotify는 **재생이 아니라 식별/매핑/내보내기** 관점으로 써야 한다.

### 3.2 Apple Music
* **관찰된 사실**
  * Apple Music API와 MusicKit은 적절한 사용자 권한이 있으면 재생, 플레이리스트 생성/수정, 라이브러리 작업을 지원한다.  
    출처: [MusicKit overview](https://developer.apple.com/musickit/), [Apple Music API overview](https://developer.apple.com/documentation/applemusicapi/), [Playlists API](https://developer.apple.com/documentation/applemusicapi/playlists-api), [MusicLibrary.createPlaylist](https://developer.apple.com/documentation/musickit/musiclibrary/createplaylist%28name%3Adescription%3Aauthordisplayname%3A%29?changes=_4)
  * Apple Music 쪽은 2025~2026년에 플레이리스트 이전 기능을 적극적으로 공식화했다.  
    출처: [YouTube Music -> Apple Music](https://support.apple.com/en-us/107776), [Apple Music -> YouTube Music](https://support.apple.com/en-us/120030), [Other services -> Apple Music](https://support.apple.com/en-us/118249)
* **해석**
  * Apple은 `공식적인 사용자 승인 기반 이동`에 우호적이며, 우리 제품의 철학인 `플랫폼 비종속 플레이리스트 자산`과 잘 맞는다.
* **제품 시사점**
  * MVP의 우선 write target 후보로 Apple Music은 충분히 유력하다.

### 3.3 YouTube / YouTube Music
* **관찰된 사실**
  * 공식적으로 확인 가능한 개발자 표면은 `YouTube Data API`이며, 플레이리스트 항목은 `videoId` 기준으로 추가된다.  
    출처: [YouTube Playlists guide](https://developers.google.com/youtube/v3/guides/implementation/playlists), [playlists.insert](https://developers.google.com/youtube/v3/docs/playlists/insert), [playlistItems.insert](https://developers.google.com/youtube/v3/docs/playlistItems/insert)
  * YouTube 정책은 YouTube 콘텐츠의 오디오/비디오 분리, 백그라운드 플레이어, 핵심 UX 복제를 금지한다.  
    출처: [YouTube Developer Policies](https://developers.google.com/youtube/terms/developer-policies)
  * 플레이리스트 생성/항목 추가는 호출당 각각 50 quota units를 사용한다.  
    출처: [playlists.insert](https://developers.google.com/youtube/v3/docs/playlists/insert), [playlistItems.insert](https://developers.google.com/youtube/v3/docs/playlistItems/insert)
* **해석**
  * `YouTube Music 전용 공개 API`보다 `YouTube 동영상 플레이리스트 API` 성격이 강하다는 점이 문제다. 이 판단은 공식 문서에서 확인 가능한 표면을 기준으로 한 **추론**이다.
  * 같은 곡이라도 공식 음원, 뮤직비디오, 라이브 영상, 유저 업로드 버전이 섞일 수 있어 정규화 난도가 높다.
* **제품 시사점**
  * YouTube는 보조 타깃으로는 쓸 수 있지만, `정규화의 기준축`으로 삼기보다는 `후순위 adapter`로 두는 편이 안전하다.

---

## 4. 대안 전략 비교 (Strategic Options)

| 전략 | 설명 | 장점 | 한계 | 판단 |
| :--- | :--- | :--- | :--- | :--- |
| `A. YouTube Music 중심 연동` | 우리 서비스는 목록을 만들고 실제 소비는 YouTube/YouTube Music으로 넘김 | 구현 직관적, 무료 사용자 풀 기대 | videoId 중심 구조, 정책 제약, 음악 식별 불안정 | **보조안** |
| `B. Player-Agnostic Metadata Ledger` | 내부에는 ISRC/플랫폼 ID/맥락 데이터만 저장하고, 실제 소비는 각 플랫폼으로 export | 저작권 리스크 가장 낮음, 플랫폼 비종속성 높음 | 매칭 엔진과 adapter 설계가 필요 | **권장안** |
| `C. Cross-platform import/export hub` | 외부 서비스 플레이리스트를 들여오고 다른 플랫폼으로 내보냄 | 유입과 활성화에 강함 | 단독 모트 약함, 플랫폼이 직접 흡수 중 | **성장 기능으로 권장** |
| `D. In-app streaming commerce` | 앱 내부 재생을 핵심 경험으로 두고 구매/선물을 붙임 | UX는 가장 매끈할 수 있음 | Spotify/YouTube 정책 리스크 큼 | **비권장** |

---

## 5. 가장 좋은 구조: `Playlist Asset Ledger + Platform Adapters`

### 제안 구조
1. **Canonical playlist asset**
   * 플레이리스트 제목
   * 설명
   * 커버 이미지
   * 일기/메모/편지
   * 감정/상황 태그
   * 인간 생성 관련 메타데이터
2. **Canonical track identity**
   * `ISRC` 우선
   * 보조 식별자: Spotify ID, Apple Music ID, YouTube videoId
   * 제목/아티스트/재생시간 기반 매칭 신뢰도
3. **Platform adapters**
   * Spotify export
   * Apple Music export
   * YouTube export
   * 가져오기는 URL/OAuth 기반으로 지원
4. **Consumption layer**
   * 앱 내부에서는 `재생`보다 `감상 페이지`, `공유`, `선물`, `저장`, `외부 앱에서 열기`
   * 필요한 경우 공식 embed/preview만 제한적으로 사용

### 이 구조가 좋은 이유
* **저작권/정책 리스크 최소화:** 음악 파일을 저장하거나 판매하지 않는다.
* **자산 독립성 확보:** 사용자가 어떤 스트리밍 서비스를 쓰든 PA는 살아남는다.
* **플랫폼 전환 마찰 제거:** "내 취향을 어디든 옮길 수 있다"는 명확한 가치가 생긴다.
* **B2B 전환 가능성 유지:** 기업 판매 시에도 음악 파일이 아니라 `사용자 맥락 + 선호 구조 + 인간성 메타데이터`를 팔 수 있다.

---

## 6. `들여오기/내보내기`를 성장 전략으로 볼 수 있는가?

### 짧은 답
**예. 다만 핵심 제품이 아니라 강한 유입 장치로 보는 것이 맞다.**

### 왜 가치가 있는가
* **관찰된 사실:** Apple과 Spotify 모두 이미 플레이리스트 이동 기능을 공식 지원하거나 제휴 방식으로 제공한다.
* **해석:** 사용자는 자신의 취향 기록이 특정 플랫폼에 갇히는 것을 싫어한다. 즉, `portability` 자체는 분명한 실수요다.
* **제품 시사점:** 우리 서비스는 "취향 자산을 해방하는 레이어"로 포지셔닝할 수 있다.

### 그러나 왜 이것만으로 부족한가
* **관찰된 사실:** 2026년 4월 현재 Apple Support는 타 서비스 -> Apple Music 이전을 공식 안내하고, Spotify도 2026년 현재 앱 내 import를 제공한다.
* **해석:** 단순 이동 기능은 시간이 갈수록 플랫폼 자체 기능으로 흡수될 가능성이 높다.
* **제품 시사점:** import/export는 **상단 퍼널**이고, 코어는 아래여야 한다.
  * 인간이 만든 플레이리스트라는 신뢰
  * 감정/기록/커버를 포함한 맥락
  * 선물/구매/컬렉션 UX
  * 플랫폼 비종속 자산화

### 가장 설득력 있는 포지셔닝 문장
> 우리는 음악을 재생하는 서비스가 아니라, 플랫폼을 넘어 이동하고 보존되며 선물될 수 있는 `취향 자산`을 만드는 서비스다.

---

## 7. 제품 포지셔닝 재정의 (Repositioning)

### 기존 위험한 정의
`플레이리스트를 사고파는 음악 서비스`

이 정의는 곧바로 음원 라이선스와 플랫폼 정책 문제를 불러온다.

### 더 나은 정의
`사용자의 플레이리스트와 감성 기록을 플랫폼 비종속 자산으로 저장하고, 어디서든 재생 가능하게 연결해 주는 취향 자산 서비스`

이 정의의 장점:
* 음악 자체를 파는 것이 아님
* 사용자가 만든 맥락과 큐레이션을 중심 가치로 삼음
* 플랫폼 이동성까지 제공해 실용 가치가 생김
* 향후 B2B에서는 `인간이 남긴 취향 구조 데이터`로 전환 가능

---

## 8. MVP 방향 제안 (Recommended MVP)

### MVP 1단계
* Spotify/Apple Music URL 또는 OAuth로 플레이리스트를 `들여오기`
* 곡들을 `ISRC + 플랫폼 ID + 텍스트 매칭`으로 정규화
* 사용자가 커버, 일기, 메모, 감정 태그를 붙여 `PA`로 저장

### MVP 2단계
* 저장된 PA를 Spotify 또는 Apple Music으로 `내보내기`
* YouTube는 후순위 adapter로 검토
* 앱 내부에서는 재생보다 `감상 페이지`와 `외부 앱에서 듣기` 중심 UX 구성

### MVP 3단계
* `선물하기`
* `랭킹/저장/좋아요`
* `공유 링크`: 수신자가 어느 플랫폼을 쓰든 자신의 앱으로 열 수 있게 유도

### MVP 4단계
* B2B용 내부 스키마 분리
* 주의: Spotify 정책을 감안하면 기업 판매 데이터셋에는 Spotify 원본 콘텐츠를 넣지 말고, 사용자 작성 맥락과 추상화된 선호 벡터 중심으로 설계해야 한다.

---

## 9. 구현 우선순위 제안 (Technical/Business Priority)

1. **정규화 레이어 구축**
   * ISRC 우선 매칭
   * 플랫폼별 ID 매핑 테이블
   * 매칭 실패/대체 트랙 처리
2. **Spotify + Apple Music 양축 지원**
   * 두 플랫폼이 현재 가장 실용적인 공식 adapter다.
3. **YouTube는 후순위**
   * 지원은 가능하되, 데이터 기준축으로 삼지 않는다.
4. **재생보다 감상/공유/내보내기 UX 우선**
   * 저작권 리스크를 낮추면서도 제품 메시지를 명확히 할 수 있다.

---

## 10. 리스크 및 장벽 (Updated Risks and Barriers)

1. **플랫폼 의존 리스크**
   * API 정책과 접근 조건은 계속 변한다. 특히 Spotify는 2026년에 개발자 접근 통제를 강화했다.
2. **매칭 오류 리스크**
   * 같은 곡의 리마스터/클린 버전/라이브 버전/지역 제한 버전이 달라 정규화 엔진 품질이 중요하다.
3. **YouTube 데이터 모델 불일치**
   * `track`이 아니라 `video` 기준이어서 음악 자산 정합성이 떨어질 수 있다.
4. **AI 데이터 판매 오해**
   * 사용자와 기업 모두 "음악을 판다"는 인상을 받으면 법적/윤리적 반발이 커진다.

---

## 11. 검증해야 할 핵심 가설 (Validation Questions)

1. 사용자는 `플랫폼 이동 가능성`만으로도 온보딩할 이유를 느끼는가?
2. 사용자는 단순 플레이리스트 이전보다 `커버 + 일기 + 편지`가 붙은 PA 저장에 더 큰 가치를 느끼는가?
3. 타인이 만든 PA를 볼 때, "내 스트리밍 앱으로 바로 열 수 있음"이 구매/선물 전환에 유의미한 영향을 주는가?
4. 기업은 곡 원본 없이도 `사용자 맥락 + 감정 태그 + 정규화된 곡 식별자 + 인간성 메타데이터` 조합에 돈을 낼 의사가 있는가?

---

## 12. 최종 제언 (Recommendation)
가장 좋은 해법은 `YouTube Music 하나에 기대는 것`이 아니라, **음악 소비는 외부 공식 플랫폼에 맡기고, 우리 서비스는 플레이리스트를 플랫폼 비종속의 감성 자산으로 만드는 것**이다.

즉, 추천 전략은 아래 조합이다.

1. **핵심 구조:** `Player-Agnostic Metadata Ledger`
2. **성장 장치:** `플랫폼 간 import/export`
3. **차별화 포인트:** `감성 맥락 + 인간 생성 증명 + 선물/구매 UX`
4. **B2B 준비:** `음원 자체가 아닌 인간 취향 구조 데이터` 중심 설계

이 방향이 가장 안전하고, 동시에 가장 제품답다.  
음악을 파는 순간 리스크가 커지지만, **취향의 맥락과 이동성을 자산화**하면 오히려 시장 진입 논리가 선명해진다.
