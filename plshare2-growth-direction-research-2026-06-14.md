# plshare2 발전 방향 — 웹 리서치 기반 전략 평가

- 작성일: 2026-06-14
- 평가 관점: 독립적·객관적 외부 평가자 (선례·시장 근거 기반)
- 현행 코어(평가 기준): `product-baseline-v2.1.md` — "감성 선물 + 무계정 즉시 재생", YouTube를 재생 기층으로 두고 음원은 직접 보유하지 않음(host-nothing), 솔로/사업자 이전(pre-business) 단계
- 검토 대상(사용자 구상 발전 트랙): (1) DM + 선물 강화, (2) 추후 독립 음원 제공, (3) SoundCloud 모방 + SNS
- 방법: 4개 축(음악 선물 / 소셜 뮤직 SNS / SoundCloud·DM 역학 / 음원 라이선스)에 대한 실제 웹 리서치. 모든 핵심 주장에 출처를 달고, 추정은 "추정"으로 표기.

---

## 0. 요약 판정

| 레이어 | 코어와의 관계 | 선례가 보내는 신호 | 판정 |
|---|---|---|---|
| 1a. 선물 강화 | **강화** (헤드라인 wedge) | 선물은 episodic·저반복 → retention 설계 필수 | **추진**(단 반복/바이럴 루프 검증 전제) |
| 1b. DM(차별점으로) | 희석 | 그래프 없는 앱의 DM은 전형적 실패; 카카오톡과 경쟁 불가 | **보류·재설계** |
| 2. 독립 음원 제공(카탈로그 호스팅) | **치명적 희석** | 라이선스 묘지; 신생사엔 사실상 불가; host-nothing 폐기 | **회피** (단 '커스텀 송' 버전만 조건부 타당) |
| 3. SoundCloud 모방 + SNS | 희석·스코프크립 | 클론 해자 없음; 일반 SNS는 묘지 | **회피** (단 Airbuds형 ambient 소셜만 조건부) |

핵심 메시지: **사용자의 "방향 본능"(선물을 강화하고, 거기에 관계/소셜 레이어를 붙여 반복을 만든다)은 리서치와 정확히 일치한다.** 문제는 그 본능을 실현할 도구로 고른 세 가지(DM·독립 음원·SoundCloud)가 각각 잘 기록된 묘지로 걸어 들어가거나 거인의 안방으로 들어간다는 점이다. 같은 비전을 근거에 맞게 구현하는 길은 따로 있다(§5).

---

## 1. 리서치로 본 선례 패턴 (근거)

### 1.1 음악 선물(gifting) 모델의 성패

지속 가능한 "선물 코어" 성공 사례는 사실상 **Songfinch 하나**다. 커스텀(주문 제작) 곡을 선물로 파는 마켓플레이스로, 2020년 $1.45M → 2021년 $5.5M → 2022년 ~$36M → 2023년 ~$75M(CEO 전망)로 성장했다([Music Ally](https://musically.com/2023/08/16/customised-songs-startup-songfinch-expects-to-make-75m-in-2023/), [Inc.](https://www.inc.com/magazine/202309/rebecca-deczynski/whats-a-custom-music-platform-anyway-chicago-startup-songfinch-has-36-million-answer.html)). 성공 조건은 다섯 가지로 압축된다: ① **노벨티가 아니라 감성/추억 깊이**에 베팅(창업자: 셀럽 노벨티 모델은 "지속 불가")([Rolling Stone](https://www.rollingstone.com/pro/news/the-weeknd-songfinch-custom-gift-music-1196163/)), ② 팬데믹 순풍, ③ **오리지널/커스텀 곡**이라 카탈로그 라이선스를 우회(아티스트가 마스터 보유, 구매자는 라이선스), ④ 2025년 American Greetings와 **유통 제휴**로 기존 선물 채널에 올라탐([PR Newswire](https://www.prnewswire.com/news-releases/american-greetings-expands-digital-gifting-offerings-302368837.html)), ⑤ 단 성장률은 둔화하고 단독 펀딩 규모는 축소(추정).

반대 극단이 **Cameo**다. 셀럽 영상 메시지(=유료 개인화 "선물")로 팬데믹기에 $1B 유니콘이 됐다가([TechCrunch](https://techcrunch.com/2021/03/30/celebrity-video-request-site-cameo-reaches-unicorn-status-with-100m-raise/)) 직원 ~400명→33명, 가치 ~$50M으로 붕괴했다([Wikipedia](https://en.wikipedia.org/wiki/Cameo_(website)), [TechCrunch](https://techcrunch.com/2023/07/18/cameo-layoffs-celebrity-greeting/)). 교훈: **노벨티+수요 충격은 폭발적이지만 남지 않는다.** 대부분 "평생 1회 구매"라 반복이 없고, 인접 수익(통화·이벤트)도 붙지 않았다([Yahoo/The Information](https://finance.yahoo.com/news/cameo-went-1-billion-unicorn-190110380.html)).

인큐번트도 선물에서 **철수 중**이다. Spotify는 소매 기프트카드 판매를 2026-03-31에 종료하고([CoinGate](https://coingate.com/gift-cards/articles/article/last-chance-spotify-gift-card-sales-officially-ending-on-march-31)), Apple의 "곡 선물하기"는 스트리밍 전환과 함께 위축됐다([Apple Support](https://support.apple.com/en-us/118401)). 선물은 저마진·사기 취약·운영 부담으로 취급된다(거인이 방어하지 않는다는 점은 신생사에 위협이자 기회).

"곡 보내기" 단독 앱은 **묘지**다: SoundTracking(인수 후 종료), Rithm(소멸), Exfm(종료)([Billboard In Memoriam 2015](https://www.billboard.com/music/music-news/in-memoriam-music-companies-2015-obit-6828956/), [TNW](https://thenextweb.com/news/rhapsodys-instagram-like-music-sharing-service-soundtracking-is-closing-tomorrow)). 사인: 인수 후 폐쇄, 실곡 전송의 라이선스 경제성, 그리고 "그냥 Spotify/YouTube 링크를 문자로 보내기"라는 무료 대체재 대비 지속 습관이 형성되지 않음.

그리고 선물은 본질적으로 **occasion 기반·저빈도**다(연하장 시장은 -2.5% CAGR로 축소, 개인화만 성장 포켓)([market.us](https://market.us/report/global-greeting-cards-market/)). 즉 매 구매가 신규 획득에 가까워 **CAC가 영구적인 적**이며, 반복은 의도적으로 설계(loyalty/occasion 리마인더/구독 레이어)해야만 생긴다([Black Hawk Network](https://blackhawknetwork.com/resources/blog/gc-egc-ungated/all/aug2025/awareness-action-building-successful-gift-card-program-strategy)).

> 시사점: 선물을 코어로 삼는 것 자체는 가능하나, ①노벨티가 아닌 감성 깊이(plshare2는 일기/맥락 보유=유리), ②유통 채널 확보, ③반복 루프 설계, ④실곡 전송이면 라이선스 청정 경로(커스텀/오리지널) 고려가 전제다.

### 1.2 소셜 뮤직 / 뮤직 SNS의 역사

뮤직 SNS는 화려한 묘지를 갖고 있다. **turntable.fm**: 2011년 바이럴(첫 달 14만 명, $35M 밸류), 2013년 폐쇄 — 원인은 **인터랙티브 라이선스 비용**(저렴한 비인터랙티브 강제 라이선스로는 상호작용을 제한·지역차단해야 했고, 정식 메이저 딜은 감당 불가)([Wikipedia](https://en.wikipedia.org/wiki/Turntable.fm), [allthingsd](https://allthingsd.com/20110621/turntable-fm-really-is-awesome-is-it-legal/)). 2021~2024년 부활판(Deepcut, Hangout)은 풀 라벨 딜+$8M대 자본을 갖췄지만 니치에 머물고 원래의 경제 문제는 미해결(추정)([MBW](https://www.musicbusinessworldwide.com/turntable-labs-raises-8-2m-to-launch-social-music-streaming-platform-hangout/)). **This Is My Jam**: 20만 명이 사랑했지만 2015년 종료 — 라이선스가 아니라 **외부 API 의존 노후화**(시간 100%를 deprecated 라이브러리 패치에 소모)와 창업자 이탈([TechCrunch](https://techcrunch.com/2015/08/09/dont-make-us-fear-the-reaper/)). **Cymbal**("Letterboxd for music"): 2018년 종료([Bandwagon](https://www.bandwagon.asia/articles/social-music-platform-cymbal-shutting-down)). **Apple Ping**: 48시간에 100만 명 후 붕괴 — 소셜 그래프 부재(페북 연동 결렬), "스토어에 소셜 겉칠"([Cult of Mac](https://www.cultofmac.com/apple-history/apple-ping-itunes-10), [iDropNews](https://www.idropnews.com/apple-history/looking-back-on-ping-apples-failed-social-media-platform/144049/)).

가장 시사적인 것은 **Spotify의 15년 지그재그**다: 2017년 인앱 메시징을 "engagement 매우 낮음"으로 폐지하고([Music Ally](https://musically.com/2017/02/28/spotify-is-removing-its-inbox-and-messaging-feature/)), 수동·앰비언트형(Friend Activity, Blend, Jam)은 유지/확장했으며, 2025년 DM을 **재출시하되 이미 Jam/Blend/공동 플레이리스트로 연결된 사람에게만** 열었다([Music Ally 2025](https://musically.com/2025/08/27/spotify-brings-messaging-back-eight-years-after-removing-it/), [Variety](https://variety.com/2025/digital/news/spotify-launches-in-app-messaging-1236497588/)). 8년의 왕복은 한 가지를 말한다: **소셜 수요는 실재하나, 실제 청취 행동에 붙어 있을 때만 남는다.**

현재의 성공작들이 이를 확증한다. **Airbuds**(위젯으로 친구의 청취를 보여줌, 1,500만 다운로드/500만 MAU)는 **메타데이터만 읽어 라이선스 책임 0** + 수동·무마찰 위젯이라 산다([TechCrunch](https://techcrunch.com/2025/09/17/airbuds-is-the-music-social-network-apple-and-spotify-wish-they-had-built/)). **Stationhead**는 **유저 본인 계정으로 재생**해 로열티 책임을 지지 않고 오히려 스트림을 만들어 라벨과 이해가 일치한다([TechCrunch](https://techcrunch.com/2023/03/09/social-music-streaming-startup-stationhead-launches-new-live-commerce-tool/)).

> 패턴: (a) **재생을 소유하면 로열티로 죽는다.** (b) **남의 카탈로그 위의 얇은 레이어가 이긴다**(메타데이터/유저계정 재생). (c) **수동·앰비언트 소셜은 남고, 개방형 메시징은 죽는다.** (d) 소셜이 목적이어야 하고(스토어 겉칠 X), 부트스트랩 그래프가 없으면 네트워크도 없다. (e) "Letterboxd for music"이 반복 실패하는 구조적 이유 — Letterboxd는 고정 라이브러리에 대한 *의견*을 다뤄 재생당 라이선스가 없지만, 음악의 재생당 라이선스가 동일 모델을 훨씬 무겁게 만든다.

### 1.3 DM/메시지 레이어의 역학

핵심 결론: **메시징은 기존의 조밀한 그래프를 증폭할 뿐, 그래프를 만들지 못한다.** Instagram DM이 성공한 것은 이미 거대한 양방향 팔로우 그래프+공유 행동이 있었기 때문이다([LeadResponse](https://leadresponse.co/blog/instagram-dm-statistics)). 반대로 Spotify는 그래프가 없어 2017년 메시징을 폐지했고(§1.2), Google+는 90% 세션이 5초 미만으로 유령도시가 됐다([Apptunix](https://www.apptunix.com/blog/why-google-failed-5-lessons-to-learn-for-entrepreneurs/)). 네트워크 제품은 **cold-start/atomic network** 문제를 먼저 풀어야 하며(가장 작은 자생적 네트워크부터)([Lenny](https://www.lennysnewsletter.com/p/atomic-network), [a16z](https://a16z.com/books/the-cold-start-problem/)), 사용자의 메시지 그래프는 이미 카카오톡/iMessage/Instagram에 있다(거대한 스위칭 코스트). 게다가 유저 간 채널을 여는 순간 첫날부터 도배·도싱 등 trust&safety 부담이 따른다(Spotify 2025 DM 도싱 이슈)([Digital Music News](https://www.digitalmusicnews.com/2025/09/04/spotify-dms-doxx-people/)).

### 1.4 SoundCloud 모델의 본질과 모방 난이도

SoundCloud의 차별점은 **공급측(크리에이터 업로드/UGC)**과 **크리에이터 수익화**(Next Pro, 60+ DSP 배급, 팬 기반 로열티)지, 카탈로그가 아니다([MBW](https://www.musicbusinessworldwide.com/as-soundcloud-overhauls-its-creator-subscription-model-ceo-eliah-seton-says-the-platform-is-building-musics-next-major-revenue-format1/)). 그러나 경제는 가혹하다: 2017년 아사 직전(직원 40% 감원)→Raine/Temasek $169.5M 구제([TechCrunch](https://techcrunch.com/2017/08/11/soundcloud-saved/)), 창립 16년 만인 2023년에야 첫 연간 흑자(매출 ~€288M에 EBITDA ~€2M, 그것도 감원하며)([EDM/MBW](https://edm.com/industry/soundcloud-first-annual-profit-16-years/)). **UGC라도 라이선스가 필요하다** — 메이저/퍼블리셔 딜 없이는 호스팅·수익화가 막히고(Universal 딜은 수년 걸림, Sony는 버팀), Universal에 일방적 삭제권까지 줬다([Techdirt](https://www.techdirt.com/2021/01/08/content-moderation-case-study-soundcloud-combats-piracy-giving-universal-music-power-to-remove-uploads-2014/)). 해자는 **양면 크리에이터 네트워크 + 복제 불가한 독점 UGC 백카탈로그(4억+ 트랙) + 문화(SoundCloud rap)**이며, 18년에 걸쳐 쌓였다. 모방자들은 정면충돌 대신 다른 일(Bandcamp=커머스, Audiomack=니치)을 골라 살아남았다([ELEVATOR](https://www.elevatormag.com/5-soundcloud-alternatives-so-that-no-one-can-technically-call-you-a-soundcloud-rapper)).

### 1.5 독립 음원 제공의 현실 장벽

온디맨드로 녹음물을 직접 제공하려면 **두 개의 저작권**(마스터=라벨/배급사 + 작곡=기계적[MLC]+공연[PRO])을 모두 클리어해야 하고, **저렴한 강제 라이선스는 비인터랙티브에만 적용**되어 곡 선택이 가능한 순간 라벨별 직접 협상이 강제된다([RIAA](https://www.riaa.com/resources-learning/licensing/), [SoundExchange](https://www.soundexchange.com/service-provider/licensing-101/)). 직접 딜은 **매출의 ~70% 유출** + 수백만 달러 최소보장(MG)/선급 + 지분 + MFN 래칫을 요구한다(Spotify는 라벨+Merlin에 지분 18%를 내줬고, Sony는 첫 3년 $42.5M 보장을 받았다)([MBW](https://www.musicbusinessworldwide.com/heres-exactly-how-many-shares-the-major-labels-and-merlin-bought-in-spotify-and-what-we-think-those-stakes-are-worth-now/), [WealthyParrot](https://www.wealthyparrot.com/why-spotify-pays-70-revenue-to-record-labels-the-streaming-music-economics-trap-explained/)). 즉 청중 0인 상태에서 출시 전에 보장금을 떠안는다.

"유저 업로드(UGC)+DMCA 세이프하버"도 공짜가 아니다 — 조건부·취약하며(Grooveshark는 직원 업로드로 세이프하버가 깨져 $736M 노출 끝에 폐쇄)([Proskauer](https://www.proskauer.com/alert/digital-music-provider-grooveshark-dismantled-in-major-victory-for-music-recording-industry)), 결국 라벨 딜과 Content ID($100M+ 구축비) 부담이 따른다. 라이선스 묘지: Grooveshark, imeem(~$30M 미지급), Rdio(Sony에 $2.4M 빚지고 파산), Beatport(첫 해 $5.5M 손실)([Billboard](https://www.billboard.com/music/music-news/rdio-bankruptcy-story-how-it-happened-failing-streaming-service-7519014/), [VentureBeat](https://venturebeat.com/business/imeem-another-music-streaming-story-ends-in-tears)). 신생사 대안을 실현 가능성 순으로 보면: **①호스팅 안 함(YouTube/Spotify로 링크/임베드) ← plshare2가 이미 택한 길**, ②권리 보유형 프로덕션 라이브러리(Epidemic Sound, $1.4B 밸류), ③인디 직접 라이선싱, ④트랙 단위 sync, ⑤UGC+Content ID, ⑥비인터랙티브 라디오, ⑦풀 카탈로그 직접 라이선싱(신생사엔 사실상 불가)([Epidemic/MBW](https://www.musicbusinessworldwide.com/epidemic-sounds-royalty-free-music-is-played-2-5bn-times-per-day-on-youtube-and-tiktok-but-company-still-struggles-to-turn-a-profit/), [Spotify oEmbed](https://developer.spotify.com/documentation/embeds/tutorials/using-the-oembed-api)). (한국: KOMCA는 작곡권만 다루고 마스터권은 별도 — 정밀 요율은 미조사로 표기.)

---

## 2. plshare2 관점의 레이어별 적합성

### 2.1 레이어 1 — 선물 강화 + DM

**선물 강화: 코어와 일치 → 추진.** 선물은 이미 v2.1의 headline wedge이므로 강화는 "맞는 곳을 더 파는" 것이다. plshare2의 감성 맥락(일기/무드/포장/언박싱)은 리서치가 지목한 *생존 요인(노벨티가 아닌 감성 깊이)*과 부합한다. 단 두 가지가 필수다. (a) **반복 루프 설계** — 선물은 저반복이므로 "선물→수신자→신규 발신자" 전환이 사실상 유일한 자생 성장 채널이다(plshare2가 무계정 재생으로 수신 마찰이 0인 점은 이 루프에 유리). (b) **유통 채널** — Songfinch의 American Greetings에 해당하는 것이 plshare2엔 **카카오톡 선물/공유**다(한국 맥락에서 가장 자연스러운 채널). 주의: 차별점이 "무료 카톡 링크 전송" 대비 충분한지(=감성 포장의 추가 가치)는 아직 미검증이며, 이것이 핵심 검증 대상이다(§4).

**DM(차별점으로): 보류·재설계.** 근거상 DM은 차별점이 아니다. (1) plshare2엔 아직 조밀한 양방향 그래프가 없다(선행 평가 기준 실사용자 0). 그래프 없는 앱에 DM을 얹는 것은 Spotify 2017·Google+가 보여준 전형적 실패다. (2) 한국에서 메시징은 카카오톡과 정면 경쟁이며 스위칭 코스트가 절대적이다. (3) 첫날부터 trust&safety(도배·도싱·미성년 보호) 부담이 생긴다. → **대안:** DM 대신 **connection primitive**(선물 답장/취향 편지/팔로우 — plshare2의 Parked 아이디어 "taste reply/correspondence"가 이미 이에 해당)로 *그래프부터* 만든다. 진짜 DM은 그래프가 생긴 뒤 Spotify 2025식(기존 연결에 게이트)으로 얹는 게 정석이다.

### 2.2 레이어 2 — 독립 음원 제공

**카탈로그 호스팅/스트리밍: 회피.** 이는 §1.5의 라이선스 묘지로 직행하는 길이며, 솔로/pre-business엔 사실상 불가능하고, **plshare2가 유일하게 구조적으로 옳게 내린 결정(host-nothing)을 스스로 폐기**하는 것이다. 권고: 하지 말 것.

**단, "독립 음원"이 커스텀/오리지널 송을 뜻한다면 조건부 타당.** Songfinch형 모델(서비스가 권리를 보유, 아티스트가 마스터 유지+구매자 라이선스)은 **라이선스 청정**하고, 커스텀 곡은 그 자체로 *궁극의 감성 선물*이라 코어를 강화하며, 명확한 수익화(객단가 높은 프리미엄 선물)가 된다. 이것이 유일하게 합리적인 "자체 음원" 경로다. 단 이는 커미션 마켓플레이스라는 **다른 사업**(양면·자본/운영 부담)이라는 점을 직시해야 한다. (인디 직접 라이선싱/UGC 업로드는 사실상 레이어 3과 합쳐진다.)

### 2.3 레이어 3 — SoundCloud 모방 + SNS

**"또 하나의 SoundCloud": 회피.** SoundCloud는 공급측(크리에이터) 플랫폼으로, "감성 선물 + 무계정 재생"이라는 소비측 제품과 **근본적으로 다른 사업**이다. 클론은 해자가 없고(§1.4), UGC라도 라이선스·Content ID 부담이 따르며, 18년·박한 흑자가 보여주듯 경제가 가혹하다. 코어를 희석하고 스코프를 폭발시킨다.

**SNS 기능: 대부분 회피, Airbuds형 ambient만 조건부.** 일반 SNS(피드·랭킹·실시간 감상방·DM)는 §1.2의 묘지 경로다. 생존형은 **Airbuds식 앰비언트**(메타데이터 기반, 무마찰, 라이선스 책임 0)이거나 **청취/선물 행동에 붙은 소셜**이다. plshare2가 특정 니치(예: 감성 선물·취향 나눔 커뮤니티)를 소유한다는 전제 하에, 그 루프를 깊게 하는 가벼운 ambient 소셜(친구가 무엇을 선물했나/무엇에 빠졌나)만이 타당하다.

---

## 3. 시퀀싱·스코프·방어력 평가

**시퀀싱이 어긋난다.** (1) DM은 위치가 틀렸다 — 그래프가 *먼저*고 DM은 그 위다. (2) "독립 음원"과 "SoundCloud"는 UGC 업로드로 가면 사실상 같은 단계로 겹친다. (3) 더 근본적으로, 세 레이어는 **세 개의 다른 사업**(소비자 선물앱 → 권리 보유 음원 제공자 → 크리에이터 UGC 플랫폼)이다. 이는 로드맵이 아니라 *추가 3회의 잠재적 피벗*이며, 선행 평가가 지적한 정체성 불안정(2개월간 4회 이상 피벗) 우려를 키운다.

**방향성의 문제.** 매 단계가 "가장 쉽고 방어 가능한 니치(host-nothing 감성 선물)"에서 **"가장 어렵고 경쟁이 치열한 커머디티"**(라이선스 벽 + 네트워크 콜드스타트 + 거인 SoundCloud/Spotify/카카오)로 이동한다. 즉 약점이 적은 곳에서 약점이 가장 많은 곳으로 행군한다.

**방어력.** "음악 선물 + 소셜"은 *기능 집합*으로는 방어되지 않는다(복제 가능). 방어력은 오직 **특정 니치/오케이전/문화의 소유 + 유통 채널(카카오톡) + 감성 깊이**에서 나온다. "감성 선물 + 한국 + 카톡 유통 + 맥락"이라는 좁은 니치가 "SoundCloud가 되자"보다 훨씬 방어 가능하다.

---

## 4. 먼저 검증해야 할 가정 (가장 중요)

새 레이어를 짓기 전에 아래 4개를 실사용으로 답해야 한다. 이 답이 없으면 모든 확장은 추측 위의 추측이다.

1. **사용자는 두 번째 선물을 보내는가?**(반복/retention — gifting-core의 생사 질문)
2. **선물→수신자→신규 발신자 루프가 실제로 전환되는가?**(K-factor — 유일한 자생 성장 채널)
3. **감성 포장이 무료 카카오톡 링크보다 더 가치 있는가?**(무마찰 대체재 대비 차별 우위)
4. **plshare2가 default가 되는 니치/오케이전이 있는가?**(방어 가능한 교두보)

---

## 5. 우선순위 권고

**P0 (새 레이어 전):**
- 선물 루프(반복 + 바이럴)를 실제 사용자 20~50명에게 검증. 신호가 나오기 전엔 DM/독립음원/SoundCloud를 **짓지 않는다.**
- 유통 채널 확보: **카카오톡 선물/공유 통합**을 1순위 성장 실험으로(Songfinch의 American Greetings 대응).

**P1 (검증 성공 시):**
- 선물 강화 + **connection primitive**(선물 답장/취향 편지/팔로우)로 *그래프를 생성*. (DM이 아니라 이것이 먼저다.)
- 소셜은 **Airbuds형 앰비언트**(친구가 무엇을 선물/청취했나)로 얇게. 동기식·SoundCloud식·실시간 감상방은 배제.
- "자체 음원"을 원한다면 **커스텀/오리지널 송 프리미엄**(Songfinch형, 라이선스 청정)으로. 카탈로그 호스팅은 아님.

**회피·연기:**
- **DM을 차별점으로 삼기** — 시기상조(그래프 부재)·카카오톡과 경쟁 불가. 그래프 생성 후 Spotify 2025식으로만.
- **독립 음원 호스팅/스트리밍** — 라이선스 묘지, host-nothing 폐기. (커스텀 송 버전만 예외)
- **SoundCloud 모방** — 다른 사업·해자 없는 클론·스코프크립.

**시퀀스 교정안:** 선물 루프 검증 → 카카오톡 유통 → connection primitive/ambient 소셜 → (선택) 커스텀 송 프리미엄 → (먼 훗날, 조밀한 그래프 + 자본이 생겼을 때에만) 크리에이터/UGC.

---

## 6. 균형: 사용자 직관 중 옳은 것

냉정한 평가지만 방향 본능 자체는 상당 부분 옳다는 점을 분명히 해둔다. (a) **선물 강화 = 맞다**(헤드라인 wedge를 더 판다). (b) **선물에 관계/소셜 레이어를 붙여 반복을 만든다는 직관 = 정확하다** — gifting 리서치가 "episodic 수요는 retention 루프로만 사업이 된다"고 똑같이 말한다. (c) **host-nothing(YouTube 기층) = 이미 최선의 선택**이다(대안 랭킹 1위). 문제는 *도구 선택*(DM·독립음원·SoundCloud)이지 방향 본능이 아니며, 같은 비전을 근거에 맞게 구현하는 길(§5)이 존재한다.

---

## 결론

세 레이어를 액면 그대로 추진하면 plshare2는 그 한 가지 구조적 강점(host-nothing 감성 선물)에서 멀어져 라이선스 벽·네트워크 콜드스타트·거인과의 정면 경쟁으로 동시에 진입한다. 그러나 사용자가 노리는 결과(선물이 반복되고 관계로 확장되는 제품)는 더 안전한 도구로 달성 가능하다: **선물 루프를 먼저 검증하고, 카카오톡을 유통으로 삼고, DM 대신 connection primitive로 그래프를 만들고, 소셜은 Airbuds처럼 얇게, "자체 음원"은 카탈로그가 아니라 커스텀 송으로.** 가장 중요한 것은 순서다 — 그래프와 반복 신호가 생기기 전의 DM·독립음원·SoundCloud는 모두 잘 기록된 실패의 재연이 될 위험이 크다.

---

## Sources

**음악 선물(gifting)**
- Rolling Stone — Songfinch 모델·펀딩·"노벨티는 지속 불가": https://www.rollingstone.com/pro/news/the-weeknd-songfinch-custom-gift-music-1196163/
- Music Ally — Songfinch 2023 ~$75M 전망: https://musically.com/2023/08/16/customised-songs-startup-songfinch-expects-to-make-75m-in-2023/
- Inc. — Songfinch "$36M" 프로필: https://www.inc.com/magazine/202309/rebecca-deczynski/whats-a-custom-music-platform-anyway-chicago-startup-songfinch-has-36-million-answer.html
- PR Newswire — Songfinch×American Greetings 유통 제휴(2025): https://www.prnewswire.com/news-releases/american-greetings-expands-digital-gifting-offerings-302368837.html
- Wikipedia — Cameo 흥망(유니콘→33명): https://en.wikipedia.org/wiki/Cameo_(website)
- TechCrunch — Cameo 유니콘: https://techcrunch.com/2021/03/30/celebrity-video-request-site-cameo-reaches-unicorn-status-with-100m-raise/ / 감원: https://techcrunch.com/2023/07/18/cameo-layoffs-celebrity-greeting/
- Yahoo/The Information — Cameo 붕괴 원인: https://finance.yahoo.com/news/cameo-went-1-billion-unicorn-190110380.html
- CoinGate — Spotify 기프트카드 종료(2026-03-31): https://coingate.com/gift-cards/articles/article/last-chance-spotify-gift-card-sales-officially-ending-on-march-31
- Apple Support — gift apps/media: https://support.apple.com/en-us/118401
- Billboard — 2015 죽은 음악 서비스(SoundTracking 등): https://www.billboard.com/music/music-news/in-memoriam-music-companies-2015-obit-6828956/
- TNW — Rhapsody, SoundTracking 종료: https://thenextweb.com/news/rhapsodys-instagram-like-music-sharing-service-soundtracking-is-closing-tomorrow
- market.us — 연하장 시장 축소·개인화 성장: https://market.us/report/global-greeting-cards-market/
- Black Hawk Network — episodic 선물의 retention 설계: https://blackhawknetwork.com/resources/blog/gc-egc-ungated/all/aug2025/awareness-action-building-successful-gift-card-program-strategy

**소셜 뮤직 / 뮤직 SNS**
- Wikipedia — turntable.fm(라이선스로 폐쇄·부활): https://en.wikipedia.org/wiki/Turntable.fm
- allthingsd — turntable.fm 비인터랙티브 라이선스 제약: https://allthingsd.com/20110621/turntable-fm-really-is-awesome-is-it-legal/
- MBW — Hangout(turntable 후신) $8.2M: https://www.musicbusinessworldwide.com/turntable-labs-raises-8-2m-to-launch-social-music-streaming-platform-hangout/
- TechCrunch — This Is My Jam 종료(API 노후화): https://techcrunch.com/2015/08/09/dont-make-us-fear-the-reaper/
- Bandwagon — Cymbal 종료: https://www.bandwagon.asia/articles/social-music-platform-cymbal-shutting-down
- Cult of Mac — Apple Ping 실패 원인: https://www.cultofmac.com/apple-history/apple-ping-itunes-10
- iDropNews — Tim Cook의 Ping 회고: https://www.idropnews.com/apple-history/looking-back-on-ping-apples-failed-social-media-platform/144049/
- Music Ally — Spotify 메시징 폐지(2017, 낮은 engagement): https://musically.com/2017/02/28/spotify-is-removing-its-inbox-and-messaging-feature/
- Music Ally — Spotify DM 재출시(2025, 기존 연결 게이트): https://musically.com/2025/08/27/spotify-brings-messaging-back-eight-years-after-removing-it/
- Variety — Spotify DM 2025: https://variety.com/2025/digital/news/spotify-launches-in-app-messaging-1236497588/
- TechCrunch — Airbuds(메타데이터 앰비언트, 15M/5M): https://techcrunch.com/2025/09/17/airbuds-is-the-music-social-network-apple-and-spotify-wish-they-had-built/
- TechCrunch — Stationhead(유저 계정 재생): https://techcrunch.com/2023/03/09/social-music-streaming-startup-stationhead-launches-new-live-commerce-tool/
- SoundExchange — 비인터랙티브 강제 라이선스 한계: https://www.soundexchange.com/service-provider/licensing-101/

**DM / 네트워크 효과**
- LeadResponse — Instagram DM 통계(그래프 증폭): https://leadresponse.co/blog/instagram-dm-statistics
- Digital Music News — Spotify DM 도싱 이슈(2025): https://www.digitalmusicnews.com/2025/09/04/spotify-dms-doxx-people/
- a16z — The Cold Start Problem: https://a16z.com/books/the-cold-start-problem/
- Lenny's Newsletter — Atomic Network: https://www.lennysnewsletter.com/p/atomic-network
- Apptunix — Google+ 실패(90% 세션 <5s): https://www.apptunix.com/blog/why-google-failed-5-lessons-to-learn-for-entrepreneurs/
- Eugene Wei — Status as a Service: https://www.eugenewei.com/blog/2019/2/19/status-as-a-service

**SoundCloud / 라이선스**
- MBW — SoundCloud 크리에이터 구독·100% 로열티·superfan: https://www.musicbusinessworldwide.com/as-soundcloud-overhauls-its-creator-subscription-model-ceo-eliah-seton-says-the-platform-is-building-musics-next-major-revenue-format1/
- TechCrunch — SoundCloud 2017 구제: https://techcrunch.com/2017/08/11/soundcloud-saved/
- EDM/MBW — SoundCloud 첫 연간 흑자(16년 만): https://edm.com/industry/soundcloud-first-annual-profit-16-years/
- Techdirt — SoundCloud×Universal 일방 삭제권(UGC도 라이선스 필요): https://www.techdirt.com/2021/01/08/content-moderation-case-study-soundcloud-combats-piracy-giving-universal-music-power-to-remove-uploads-2014/
- RIAA — 온디맨드는 마스터 직접 라이선스 필요: https://www.riaa.com/resources-learning/licensing/
- MBW — 라벨+Merlin이 Spotify 지분 18% 취득: https://www.musicbusinessworldwide.com/heres-exactly-how-many-shares-the-major-labels-and-merlin-bought-in-spotify-and-what-we-think-those-stakes-are-worth-now/
- WealthyParrot — ~70% 유출·MG·Sony $42.5M 보장: https://www.wealthyparrot.com/why-spotify-pays-70-revenue-to-record-labels-the-streaming-music-economics-trap-explained/
- Proskauer — Grooveshark 해체($736M 노출): https://www.proskauer.com/alert/digital-music-provider-grooveshark-dismantled-in-major-victory-for-music-recording-industry
- Billboard — Rdio 파산(라이선스 비용): https://www.billboard.com/music/music-news/rdio-bankruptcy-story-how-it-happened-failing-streaming-service-7519014/
- VentureBeat — imeem 붕괴: https://venturebeat.com/business/imeem-another-music-streaming-story-ends-in-tears
- MBW — Epidemic Sound(권리 보유형, 2.5bn/day): https://www.musicbusinessworldwide.com/epidemic-sounds-royalty-free-music-is-played-2-5bn-times-per-day-on-youtube-and-tiktok-but-company-still-struggles-to-turn-a-profit/
- Spotify oEmbed(호스팅 안 함 경로): https://developer.spotify.com/documentation/embeds/tutorials/using-the-oembed-api
- Wikipedia — KOMCA(작곡권 중심, 한국): https://en.wikipedia.org/wiki/Korea_Music_Copyright_Association

*주: 일부 수치(Songfinch 2023 매출, turntable 후신 현재 MAU, KOMCA 정밀 요율)는 출처상 전망치이거나 미공개/미조사로, 본문에 추정·미조사로 표기함.*
