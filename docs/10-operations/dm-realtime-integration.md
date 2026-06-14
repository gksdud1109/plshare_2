# DM 실시간(Realtime) 전환 설계 — Supabase Realtime

> 상태: **설계 확정 / 구현 대기(프로비저닝 의존)**. 전송중립 위생(since-cursor 증분 읽기 + 읽음 처리 분리)은 이미 머지됨 → Realtime은 폴링 `poll()` 만 구독 콜백으로 바꾸는 작은 변경으로 떨어진다.

## 0. 배경 — 왜 지금 코드를 안 쓰고 설계만 두는가
- 라이브 검증이 **Supabase 프로비저닝 + 인증 모델 결정**에 막혀 있다(아래 1·2). 검증 불가능한 구독 코드를 미리 쓰면 그 두 결정에 따라 재작성된다.
- 위생 PR로 **전송계층 중립**은 이미 확보됨: FE 스레드는 `초기 전체 로드 → poll(after=커서) → mergeMessages → markRead` 구조라, Realtime은 `poll()` 자리에 채널 구독을 끼우면 병합·읽음처리 로직을 그대로 재사용한다.

## 1. 아키텍처
```
[FE] 보내기 ─POST /api/conversations/{id}/messages─▶ [Spring BE] ─INSERT─▶ [Supabase Postgres]
[FE] 읽음   ─POST /api/conversations/{id}/read────▶ [Spring BE]                    │ (logical replication)
[FE] 구독   ◀──────────── Supabase Realtime (postgres_changes: messages) ─────────┘
```
- **쓰기·읽음·초기 로드는 그대로 Spring BE**(소스 오브 트루스 유지). Realtime은 **푸시 채널만** 담당.
- BE가 `messages` 에 INSERT → Supabase Realtime이 구독 중인 FE에 자동 푸시. **백엔드 fan-out 코드 0.**
- 다중 인스턴스 fan-out(pub/sub 백플레인)도 Supabase Realtime이 관리 → 별도 Redis 불필요.

## 2. 핵심 결정 — 인증 브리지 (custom 세션 ↔ Supabase Realtime)
plshare 인증은 **자체 HMAC 세션**(`ApplicationSessionService`, Google OAuth)이고 **Supabase Auth가 아니다.** Supabase Realtime의 per-user 권한(postgres_changes RLS / Realtime Authorization)은 Supabase JWT(`auth.uid()`)를 전제한다. 세 가지 옵션:

| 옵션 | 내용 | 평가 |
|---|---|---|
| A. Supabase Auth 채택 | 자체 세션 시스템을 Supabase Auth로 교체 | ❌ 과함 — 검증된 auth 전면 교체 |
| B. **BE가 Supabase 호환 JWT 발급** | BE가 Supabase JWT secret으로 `sub=userId` JWT를 서명 발급 → FE가 그걸로 Realtime 접속 → RLS `auth.uid()` 동작 | ✅ **추천** — 자체 auth 보존 + per-user 스코프 |
| C. Broadcast 공개 채널 | RLS 없이 `conversation:{id}` 채널 broadcast | ⚠ 권한 누수(anon key 가진 누구나 구독) — 비추천 |

**추천: 옵션 B.** BE에 `GET /api/auth/realtime-token`(인증 필수) 추가 → 시드된 Supabase JWT secret으로 `{ sub: userId, exp: +N분 }` 서명. FE는 이 토큰으로 `supabase.realtime.setAuth(token)` 후 구독. 자체 세션 시스템은 그대로.

## 3. 프로비저닝 체크리스트 (사용자 작업)
- [ ] Supabase 프로젝트 생성, **prod DB를 Supabase Postgres로** (Flyway 마이그레이션을 거기에 적용 → `messages` 테이블 존재)
- [ ] `messages` 테이블에 Realtime(logical replication) 활성화 + publication 등록
- [ ] RLS 정책: 사용자는 자신이 참여한 conversation의 message만 SELECT(= Realtime 수신). `conversations.participant_a_id/b_id` 와 `auth.uid()` 조인 기반
- [ ] env: `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`(FE, 이미 `lib/env.ts`에 자리 있음), `SUPABASE_JWT_SECRET`(BE, realtime-token 서명용)

## 4. 구현 단계 (프로비저닝 후 — 작고 검증 가능)
1. BE: `GET /api/auth/realtime-token` (옵션 B JWT 발급).
2. FE: `npm i @supabase/supabase-js`; `lib/supabase/client.ts` 플레이스홀더를 실제 클라이언트로 채움.
3. FE: `useRealtimeMessages(conversationId)` 훅 — realtime-token으로 `setAuth` → `postgres_changes`(event INSERT, filter `conversation_id=eq.{id}`) 구독 → 수신 row를 `MessageDto` 매핑 → 기존 `mergeMessages` 로 병합 + 상대 메시지면 `markConversationRead`.
4. FE 스레드: env 있으면 `useRealtimeMessages`, 없으면 기존 `poll()` 폴백(플래그 게이트 → 점진 전환·로컬 개발 무영향).

## 5. 검증 계획 (프로비저닝 후 — 그때 직접 수행)
- 두 브라우저 컨텍스트(유저 A·B) → A가 보낸 메시지가 B 스레드에 **폴링 간격 없이 즉시** 도착.
- env 제거 시 폴링 폴백 정상(회귀 0).
- RLS: 제3자(비참여자) 토큰으로 구독 시 수신 0(권한 격리).
- 재연결: 네트워크 끊김 후 `@supabase/supabase-js` 자동 재연결.

## 6. 손수 Spring WebSocket은 언제?
함께듣기 재생위치 **sub-100ms 서버권위 동기**·협업큐 동시편집 충돌해결처럼 Supabase의 클라-브로드캐스트 모델로 안 되는 **서버권위 저지연** 코어에 한해. 그 외(DM 푸시·presence·리액션)는 Supabase Realtime으로 충분. 이 판정은 해당 기능 스펙 시점에.
