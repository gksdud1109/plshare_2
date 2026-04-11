---
name: claude-api-integration
description: Integrate Claude API into backend tasks that benefit from LLM reasoning—track normalization fallback, emotional tag extraction, playlist description summarization, proof-of-human context scoring. Uses official Anthropic SDK, default model claude-opus-4-6, adaptive thinking.
role: Backend Engineer
source: adapted from https://github.com/anthropics/skills/tree/main/skills/claude-api
---

# Skill: Claude API Integration

## Purpose
PA 제품에는 LLM이 없으면 품질이 급락하는 지점이 있다:
- ISRC 매칭 실패 곡의 후보 재정렬 (제목·아티스트 fuzzy 매칭).
- 사용자 일기/메모에서 감정 태그 및 맥락 키워드 추출.
- 공유 페이지용 플레이리스트 요약 카피 생성.
- 휴먼 컨텍스트 진위도 스코어링 보조 신호.

이 skill은 그 통합을 "공식 SDK · 최신 모델 · 캐싱"이라는 최소 안전 규칙에 묶는다.

## When to use
- 위 4개 영역 중 하나를 새로 구현/개선할 때.
- 기존에 다른 공급자(OpenAI 등)로 짜여 있는 LLM 호출을 이관할 때.
- `docs/prd/functional-requirements-v*.md`가 "AI 보조"를 요구하는 구간.

## When NOT to use
- 결정적 로직(정규 매칭, DB 조회, 계산)에 LLM을 억지로 끼우는 경우.
- 사용자 PII를 본문에 그대로 주입하는 경우 — 먼저 마스킹 정책 결정.
- 프론트엔드에서 직접 API key를 호출하는 경우 — 항상 서버 사이드.

## Required inputs
1. 유스케이스 한 문장 (예: "플레이리스트 10곡을 2문장 감성 요약으로").
2. 입력 스키마·출력 스키마.
3. SLA(지연, 초당 호출), 예산(월 비용 상한).
4. PII/저작권 민감 필드 리스트.
5. 언어(Python/TypeScript/기타).

## Workflow (exact steps)

### Step 1 — 사전 스캔
1. 레포에서 `openai`, `gpt-4`, `OPENAI_API_KEY` 문자열을 검색. 존재하면 멈추고 이관 여부를 PO와 확인.
2. 이미 `@anthropic-ai/sdk` 또는 `anthropic`이 설치됐는지 확인. 없으면 설치.

### Step 2 — 모델·파라미터 고정
- **모델**: `claude-opus-4-6` (사용자가 명시적으로 다른 걸 요구하지 않는 한).
- **Thinking**: Opus 4.6에서는 `thinking: { type: "adaptive" }` 사용. `budget_tokens` 지정 금지.
- **Streaming**: 사용자 대면이면 streaming, 배치면 non-streaming.
- **Temperature**: 추출 작업은 0~0.2, 카피 생성은 0.6~0.8.

### Step 3 — 프롬프트 캐싱 설계
모든 호출은 가능한 한 prompt caching을 사용한다.
- 시스템 프롬프트 + 도메인 지침(`docs/prd/playlist-first-product-intent.md` 요약)을 `cache_control: { type: "ephemeral" }`로 캐시.
- 요청별 변하는 값(곡 목록, 사용자 일기)만 캐시 밖으로.
- 캐시 hit률을 로그로 남긴다.

### Step 4 — 도구 사용 여부 결정
세 가지 surface 중 하나를 고른다:
1. **단일 호출**: 분류·추출·요약 → 대부분 여기.
2. **Tool use**: 곡 후보 DB 조회가 중간에 필요한 경우.
3. **Managed Agents (beta)**: 현재 MVP에서는 비권장. 상태 보존 필요 시 재평가.

### Step 5 — 스키마 강제
- 출력 JSON이 필요하면 schema를 시스템 프롬프트에 명시 + 코드에서 zod/pydantic으로 parse.
- parse 실패 시 1회만 재시도. 2회 실패는 fallback 경로로.

### Step 6 — 안전·비용 가드
- 요청 본문에 PII 마스킹 적용.
- 월 호출 수·토큰 수에 상한을 두고, 상한 근접 시 feature flag off.
- 모든 호출에 `user_id` 해시를 metadata로 기록.

### Step 7 — 테스트
- 입력/출력 샘플 10개로 eval harness 구성.
- 회귀는 `tests/ai/<feature>.spec.ts`로 저장.
- Prompt 변경 시 eval 전부 재실행.

### Step 8 — 문서화
`docs/implementation/decision-log.md`에 1 섹션 추가:
- 유스케이스, 모델, thinking 옵션, 캐시 전략, eval 결과, 예상 비용.

## Expected outputs
- 서버 사이드 모듈 (`lib/ai/<feature>.ts` 등).
- `tests/ai/*.spec.ts` eval harness.
- `docs/implementation/decision-log.md` 갱신.
- 비용·지연 모니터링 훅.

## Role mapping
- **Primary**: Backend Engineer (Codex CLI).
- **Reviewer**: Reviewer/Operator가 eval 결과와 비용 가드 확인.

## Handoff to next role
- → **Frontend Engineer**: API 계약(입출력 스키마)만 전달. 프롬프트 내부는 노출하지 않는다.
- → **PO**: 새 AI 기능이 PRD에 명시되지 않았다면 `prd-sharpening`으로 돌려 먼저 요구사항에 반영.
- 금지: Frontend에서 직접 Anthropic API key 호출. 금지: 프롬프트를 client bundle에 포함.
