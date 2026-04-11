# Market Research v0.1

## 문서 정보
- Date: `2026-04-11`
- Scope: `playlist-first`, `human-authenticity`, `consumer-to-enterprise data value`
- Based on: `docs/20-product/strategy/archive/prd-v0.1.md`, `docs/20-product/requirements/playlist-first-product-intent.md`

---

## 1. 조사 목적
이 문서는 `plshare2`의 첫 제품 전략인 `플레이리스트 데이터화`가 시장에서 어디에 위치하는지 정리한다.

검토 질문은 3가지다.
- 이미 사람들이 플레이리스트를 만들고 공유하는가
- 감성적/수집적/거래적 경험으로 확장될 여지가 있는가
- 기업이 실제로 `진짜 인간 데이터`에 비용을 지불하는 시장이 존재하는가

---

## 2. 핵심 시장 가설

### 가설 A. 플레이리스트는 이미 사회적 객체다
Spotify는 공개 플레이리스트를 프로필에 게시할 수 있고, 공유된 플레이리스트는 다른 사용자가 재생, 팔로우, 재공유할 수 있다. 이는 플레이리스트가 이미 단순 기능이 아니라 사회적 콘텐츠라는 뜻이다.

해석:
- `생성 -> 공개 -> 공유 -> 팔로우`는 이미 검증된 행동이다.
- 따라서 우리 제품은 `플레이리스트 생성 행동` 자체를 새로 만들기보다, 그 행동에 `감정 맥락`과 `자산성`을 덧씌우는 전략이 유효하다.

---

### 가설 B. 음악은 수집품과 팬 정체성으로 확장될 수 있다
Sound.xyz는 음악을 디지털 수집품으로 판매하고, 수집자가 댓글을 남기고 보상을 받으며, 소유 기록을 보여주는 구조를 운영하고 있다.

해석:
- 음악과 소유, 커뮤니티, 정체성 결합은 시장적으로 낯선 개념이 아니다.
- 다만 Sound.xyz는 `아티스트 중심 음원 수집`에 가깝고, 우리 제품은 `개인 큐레이션 플레이리스트 자산`이라는 점에서 포지션이 다르다.

---

### 가설 C. 랭킹과 보상은 음악 참여도를 높인다
FanLabel은 리더보드와 보상을 활용해 음악 팬 참여를 게임화하고 있다.

해석:
- 플레이리스트에도 `랭킹`, `획득`, `선물`, `희소성` 메커니즘을 얹을 수 있다.
- 다만 경쟁만 강조하면 감성 자산이라는 서사가 깨질 수 있으므로, 리더보드는 `인기`보다 `큐레이션 가치`와 `스토리성`을 보여주는 방향이 더 적합하다.

---

### 가설 D. 기업은 검증된 인간 데이터를 실제로 구매한다
Prolific은 검증된 실제 인간 참여자 기반으로 AI 평가와 학습용 human data를 판매하고 있고, `verified participants`, `quality checks`, `anti-bot/anti-AI safeguards`를 핵심 가치로 내세운다.

해석:
- `사람이 만든 고품질 데이터`에 돈을 지불하는 수요는 실재한다.
- 우리 제품의 차별점은 설문/태스크 응답형 human data가 아니라 `취향`, `감정`, `맥락`, `멀티모달 기록` 기반 human data라는 점이다.

---

### 가설 E. 원본성/출처 증명 레이어는 산업 표준과 연결할 수 있다
C2PA는 Content Credentials 기술 규격을 계속 발전시키고 있다.

해석:
- 완전한 증명 시스템을 즉시 만들기 어렵더라도, `출처`, `생성 이력`, `검증 가능 메타데이터`를 제품 구조에 넣는 방향은 산업 흐름과 맞다.
- 특히 사진/커버 이미지 쪽에는 향후 C2PA 또는 유사 provenance 구조를 연결할 여지가 있다.

---

## 3. 경쟁 구도

## A. 대형 스트리밍 플랫폼
대표:
- Spotify
- Apple Music
- YouTube Music

강점:
- 기존 사용자 행동과 음악 라이브러리
- 플레이리스트 생성/공유의 거대한 분모

한계:
- 플레이리스트를 `개인 자산`으로 다루지 않음
- 감성 기록, 편지, 일기, 커버 맥락이 약함
- 사용자에게 데이터 판매 수익이 귀속되지 않음

우리의 기회:
- 스트리밍 서비스를 대체하지 않고, 그 위에 `감정 자산 레이어`를 올린다.

---

## B. 음악 수집품 / 웹3 음악 플랫폼
대표:
- Sound.xyz
- Sweet

강점:
- 소유, 희소성, 보상, 커뮤니티 구조 경험

한계:
- 개인의 일상적 플레이리스트 큐레이션보다는 아티스트/브랜드 중심
- 일반 대중의 반복 기록 행동과는 거리 있음

우리의 기회:
- `전문 아티스트 음원 소유`가 아니라 `일상 큐레이션의 감정 가치`를 거래 단위로 만든다.

---

## C. 음악 커뮤니티 / 소셜 플레이리스트 앱
대표:
- Mixtape Social Music Playlists
- Spotify 공유/메시지 기능

강점:
- 플레이리스트를 대화와 정체성 표현 수단으로 사용

한계:
- 구매/선물/자산화 구조가 약함
- 기업용 데이터 상품으로 연결되지 않음

우리의 기회:
- `사회적 객체`인 플레이리스트를 `감정 기록 자산`으로 확장

---

## D. 인간 데이터 플랫폼
대표:
- Prolific
- Scale AI 같은 데이터 인프라 사업자

강점:
- 고품질 human data 수요를 기업 예산으로 연결
- 검증, 품질, 참여자 보상 구조가 명확

한계:
- 일상 취향과 문화 데이터를 자산화하지 않음
- 음악/감정/멀티모달 기록이라는 높은 맥락성은 상대적으로 약함

우리의 기회:
- `취향 기반 라이프로그`를 B2B 데이터셋으로 구조화

---

## 4. 시장 기회 해석

### 소비자 시장
이미 사람들은 플레이리스트를 만들고 공유한다.
따라서 소비자 시장의 진입 포인트는 `새 행동 유도`가 아니라 `기존 플레이리스트 행동의 감성적 고급화`다.

핵심 질문:
- 왜 이 플레이리스트를 단순 공유가 아니라 `선물`하고 싶어지는가
- 왜 이 플레이리스트를 `구매`할 만큼 특별하다고 느끼는가

결론:
- 구매 이전에 `정서적 패키징`이 먼저 필요하다.
- 플레이리스트 페이지는 링크 목록이 아니라 `감정 편지`처럼 느껴져야 한다.

---

### 기업 시장
기업은 단순 곡 목록보다 `검증 가능한 인간 맥락 데이터`에 더 높은 가치를 둘 가능성이 있다.

핵심 질문:
- 감정 태그와 일기, 이미지, 큐레이션 순서가 모델 학습에서 얼마나 유의미한가
- 이 데이터의 권리/동의/익명화 체계를 얼마나 명확히 설계할 수 있는가

결론:
- B2B는 초기 직접 매출보다 `데이터셋 준비도`를 제품 구조에 심는 것이 우선이다.
- MVP에서 중요한 것은 판매 API보다 `구조화 가능한 데이터 스키마`다.

---

## 5. 제품 포지셔닝 제안

한 줄 포지셔닝:
`플레이리스트를 감정 자산으로 만들고, 인간 진본 데이터를 개인의 수익 기회로 전환하는 서비스`

비교 포지션:
- Spotify보다 더 감정적이고 기록 중심
- Sound.xyz보다 더 일상적이고 큐레이션 중심
- Prolific보다 더 문화적이고 멀티모달한 human data 중심

---

## 6. 추천 전략

### 추천 1. MVP는 플레이리스트 선물 경험에 집중
구매보다 선물이 초기 진입장벽이 낮다.

이유:
- 감성 서사와 잘 맞음
- 가격 저항이 낮음
- 거래보다는 관계 행동에 가까워 초기 바이럴 가능성이 있음

---

### 추천 2. 랭킹은 거래성보다 큐레이션 가치를 보여주는 방향으로 설계
초기 랭킹 지표 후보:
- 저장 수
- 선물 수
- 완독률 또는 상세 페이지 체류
- 감정 공감 반응 수

---

### 추천 3. B2B는 즉시 판매보다 데이터 패키징 준비
초기부터 아래 필드를 구조화해야 한다.
- 음악 메타데이터
- 감정 태그
- 텍스트 기록
- 이미지 메타데이터
- 생성/수정 시각
- 인간 생성 증명 점수
- 익명화 상태
- 판매 가능 동의 상태

---

## 7. 남는 리스크
- 음악 메타데이터/재생 연동 관련 플랫폼 정책
- 사용자 생성 콘텐츠의 권리 귀속과 2차 판매 표현 방식
- 민감한 감정 기록의 기업 판매에 대한 거부감
- 인간 생성 증명의 정확도와 설명 가능성

---

## 8. 결론
시장 관점에서 이 제품은 `새로운 음악 앱`이라기보다 세 시장의 교차점에 있다.

- 소셜 플레이리스트
- 감정 기록 UX
- 검증 가능한 human data

따라서 초기 제품은 `플레이리스트를 감정적으로 선물하고 수집하게 만드는 소비자 경험`을 만들고, 그 아래에서 `기업 판매 가능한 데이터 구조`를 축적하는 방향이 가장 타당하다.

---

## Sources
- Spotify playlist publishing and sharing: [Spotify Support](https://support.spotify.com/na-af/article/playlist-publishing/)
- Spotify sharing features: [Spotify Newsroom, November 7, 2025](https://newsroom.spotify.com/2025-11-07/share-spotify-music-whatsapp-instagram-tiktok/)
- Sound.xyz overview: [Sound.xyz Help](https://help.sound.xyz/hc/en-us/articles/5304493670939-What-is-Sound-xyz)
- Sound.xyz collector value: [Sound.xyz Help](https://help.sound.xyz/hc/en-us/articles/15757578142235-What-is-the-collector-getting-when-they-buy-my-song)
- Sound.xyz tier and collector perks: [Sound.xyz Help](https://help.sound.xyz/hc/en-us/articles/17543164476443-How-do-Tiers-work)
- FanLabel market signal: [FanLabel](https://fanlabel.com/)
- Prolific human data positioning: [Prolific](https://www.prolific.com/), [About Prolific](https://www.prolific.com/about), [Data Quality](https://www.prolific.com/data-quality)
- C2PA specification: [C2PA Specification 2.4](https://spec.c2pa.org/specifications/specifications/2.4/specs/C2PA_Specification.html)
