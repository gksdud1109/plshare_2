# 기술부채 재발 방지 리뷰 룰셋

이 룰셋은 일반론이 아니다. 실제 코드 리뷰(polyenm_pan / ocb-prepoint / plshare)에서 반복 발견된
패턴을 룰로 굳힌 것이다. 각 룰의 **유래**가 실제 버그다. `debt-reviewer` 에이전트가 이 파일을 읽고
변경분(git diff)에 적용한다. 사람이 직접 PR 체크리스트로 써도 된다.

핵심 프레임: **"속도냐 품질이냐"가 아니라 "지금 안 한 게 나중에 한 줄이냐 전면 재작성이냐"다.**
모든 발견은 회수 비용으로 분류한다:
- **[회수비쌈]** = 아키텍처의 *모양*을 결정한다(신원 흐름, 트랜잭션 경계, 도메인 모델). 미루면 전면 재작성.
  → demo/프로토타입이어도 **최소 골격은 둔다.** 이걸 미루는 건 속도가 아니라 부채의 이자다.
- **[회수가능]** = 나중에 국소적으로 추가한다(암호화, 입력검증, 테스트, 튜닝).
  → demo에서 의도적 생략 **정당.** AI 레버리지에 위임해도 된다.

---

## R0 — 맥락을 먼저 확인한다 (severity 매기기 전)

표면만 보고 단정하지 않는다. severity를 매기기 전에:
- `git log`/`git blame` — 커밋된 코드냐, 아직 정리 안 한 working tree냐?
- 활성 프로필 / `application*.yml` — demo냐 prod냐? Mock/Real이 프로필로 게이팅되나?
- 주변 주석/TODO — 의도된 생략(회수 예정)이냐, 모르고 빠뜨린 거냐?

**유래:** 리뷰어가 `ServiceCopy.kt`를 "커밋된 죽은 코드"로 단정했으나 실제론 압축해 온 working
tree 잔재였다. 맥락 없이 표면으로 단정하면 리뷰어도 틀린다. 이 룰을 통과 못 하면 나머지 룰의
severity가 전부 부정확해진다.

---

## R1 — 신원(actor)은 요청에서 받지 않는다  [회수비쌈]

컨트롤러/서비스가 `@RequestParam userId`, `body.grantId`, `body.senderId` 등으로 **행위 주체의
신원**을 받으면 🔴. 신원은 인증 컨텍스트(SecurityContext / 인증된 principal)에서만 온다.

- 탐지: `@RequestParam(.*[Uu]ser[Ii]d|.*grantId|.*requesterId)`, `body.userId`, `SaveXxxRequest(val userId`
- 왜 회수비쌈: 모든 컨트롤러 시그니처와 프론트 호출이 이 신원 전달에 결합된다. 나중에 진짜
  인증을 넣으려면 전 컨트롤러 + 전 프론트 호출 재작성. demo여도 "가짜라도 SecurityContext 경유"
  골격만은 둔다 → 나중이 한 줄이 된다.
- 모범: polyenm_pan의 `CurrentUserId` ArgumentResolver + `authenticated()`.

**유래:** plshare `SecurityConfig`가 `/** permitAll` + 전 컨트롤러 actor-from-request → 무인증 mass IDOR.

---

## R2 — 거부/기록을 부수효과의 결과에 종속시키지 않는다  [회수비쌈]

상태 전이·성공 기록이 격리 블록(`runCatching`/`try`) **밖**에 있거나, 거부(`throw`)가 부수효과의
성공/실패/행수에 매달리면 🔴. 불변식(거부, 기록)은 부수효과와 **독립**이어야 한다.

- 탐지: `.onSuccess {` 블록 안의 DB 쓰기(격리 밖), `catch (...) { throw }`(거부가 실패에 종속),
  `if (repo.updateXxx(...) > 0) { throw }`(거부가 행수에 종속).
- 점검 질문: 부수효과가 성공/실패/0행일 때 각각 거부와 기록이 의도대로 일어나나? 한 줄씩 따라가라.
- 모범: 부수효과는 별도 `REQUIRES_NEW`로 커밋 격리, 거부는 그 뒤 무조건 throw.

**유래:** ① ocb `completePointPayout`+`commit()`이 `runCatching` 밖 → 기록 실패 시 배치 전체 중단 +
돈은 나갔는데 추적 불가. ② polyenm `rotate()`가 reuse-detection 폐기를 같은 tx throw로 롤백.

---

## R3 — DB 트랜잭션이 외부 네트워크 호출을 감싸지 않는다  [회수비쌈]

`@Transactional` / `transaction{}` 경계 **안**에서 외부 HTTP 호출(SOI/PG/REST/WebClient)이 일어나면 🔴.
커넥션을 쥔 채 네트워크를 기다려 풀 고갈 → 앱 전체로 전파.

- 탐지: 트랜잭션 메서드 본문의 `.block()`, `restClient`, `webClient`, `httpClient`, `feign`, `RestTemplate`.
- 모범: 짧은 tx로 상태 선점 후 커밋(커넥션 반납) → tx 밖에서 네트워크 호출 → 다시 짧은 tx로 결과 기록.

**유래:** ocb 지급 배치 전체가 `@ExposedTransaction`인데 루프 안에서 SOI HTTP 호출 → 커넥션 1개를
배치 내내 점유. (이력서에 "외부호출과 DB 트랜잭션 분리"라 써놓고 이 코드에선 깨진 케이스.)

---

## R4 — 쓰기/상태 전이는 멱등하거나 동시성 가드를 가진다  [상황에 따라]

상태 전이/INSERT에 `WHERE status=?` 가드나 unique 제약이 없으면 검토. 동시 요청·재실행·중복
요청에서 무엇이 최종 진실인가? read-then-write 사이 race는?

- 탐지: `update(...)` without status guard, save with no unique constraint, `existsBy` 후 `save`(TOCTOU).
- 모범: 상태 가드 `UPDATE ... WHERE id=? AND status=?`(atomic CAS), unique 제약을 최종 방어선으로,
  경쟁 패자는 `DuplicateSafeInserter`(REQUIRES_NEW)로 no-op.
- 회수비용: 금전/발급/정산이면 **[회수비쌈]**(이중지급은 사후 회수 위험). 단순 소셜 액션이면 [회수가능].

**유래:** ocb claim-우선 패턴(잘 된 예) vs plshare Gift `save()` last-write-wins(비멱등).

---

## R5 — 토큰/시크릿은 평문으로 저장·전송·로깅하지 않는다  [대부분 회수가능, URL 노출은 비쌈]

OAuth 토큰/시크릿/세션 식별자를 평문 DB 컬럼(`@Convert` 없음), URL 쿼리스트링, 로그에 노출하면 🟠~🔴.

- 탐지: 토큰 컬럼에 converter 없음, `?session=`/`?token=`/`?grantId=` redirect, `log.*token`,
  하드코딩 자격증명, 기본 비밀번호.
- 회수비용: 평문 저장 → [회수가능](나중에 AttributeConverter). **URL/세션 식별자 노출 → [회수비쌈]**
  (식별자가 곧 자격증명이면 referer/로그/히스토리로 새고, 흐름 자체를 바꿔야 함).

**유래:** plshare OAuth 토큰 평문 DB 저장 + grantId가 redirect URL에 노출(grantId가 곧 bearer).

---

## R6 — demo/Mock이 prod 경로로 새지 않는다  [회수가능, 단 배포 가드 필수]

데모 토큰/fixture가 프로필 게이팅 없이 prod 코드 경로에 있거나, 기본 활성 프로필이 `demo`라
배포 시 프로필 미지정으로 Mock이 prod에서 도는 위험이 있으면 🟠.

- 탐지: `"demo-`/`"test-` 하드코딩 토큰(컨트롤러/서비스), `active: demo`(기본 프로필),
  fixture가 실 UI 경로의 fallback.
- 모범: Mock/Real `@Profile` 게이팅(plshare가 잘 함) + 기본 프로필을 안전한 쪽으로 또는 배포 가드.

**유래:** plshare `demo-youtube-token` 하드코딩 + 기본 프로필 `demo`. (Mock/Real 프로필 분리 자체는 모범.)

---

## R7 — AI 레버리지 산출물은 "표면 통과"를 신뢰로 읽지 않는다  [메타]

생성된 코드가 컨벤션·구조상 그럴듯해도, **아키텍처 모양을 결정하는 것(R1·R2·R3)은 사람이 제약으로
박고 직접 검증**한다. 회수가능 부채는 위임 OK. 표면 품질과 검증된 정합성은 다른 축이다.

- 점검: 이 변경에서 신원 흐름/트랜잭션 경계/예외 경계가 "생성된 그대로"인가, "검증된" 것인가?
  생성된 그대로면 R1~R3을 라인 단위로 따라갔는지 확인.

**유래:** plshare가 컨벤션·Mock/Real 분리는 화려한데 인증(아키텍처 모양)이 통째로 비었다.
검증 없는 레버리지는 그럴듯한 구멍을 양산한다.

---

## 출력 규칙 (debt-reviewer가 지킨다)

1. **R0 맥락 확인 결과를 먼저 보고** (프로필/working-tree/주석).
2. 각 발견: `[R번호][회수비쌈|회수가능] severity(🔴🟠🟡) file:line — 구체 실패 시나리오 — 수정 방향`.
3. **[회수비쌈] 발견이 하나라도 있으면 demo여도 `BLOCK`.** [회수가능]만 있으면 demo에선 `NOTE`(생략 정당).
4. 헛경보 방지: 가드가 다른 곳에 있는지, 프로필로 게이팅되는지 확인 후 보고(R0).
