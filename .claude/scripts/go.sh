#!/bin/zsh
# go.sh — 단일 진입점
# 큐 동기화 + 승인 자동 처리(approve-all) + pending 태스크 목록 표시
# 사용법: zsh .claude/scripts/go.sh
set -uo pipefail

QUEUE_FILE="${QUEUE_FILE:-docs/10-operations/tasks/task-queue.yaml}"
CMD="./.claude/scripts/task_queue.rb"

cd "$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

echo "================================================================"
echo "  plshare2 — go"
echo "  $(date '+%Y-%m-%d %H:%M')"
echo "================================================================"

# 1. 의존성 해소된 blocked 태스크 → pending (또는 명시적 awaiting_approval)
UNBLOCKED="$("$CMD" "$QUEUE_FILE" unblock-ready 2>/dev/null || true)"
if [[ -n "$UNBLOCKED" ]]; then
  echo ""
  echo "▶ unblocked"
  echo "$UNBLOCKED" | sed 's/^/  - /'
fi

# 2. 잔존 awaiting_approval 일괄 승인 (P0 전략 변경처럼 명시 true가 아니면 즉시 pending)
APPROVED="$("$CMD" "$QUEUE_FILE" approve-all 2>/dev/null || true)"
if [[ -n "$APPROVED" ]]; then
  echo ""
  echo "▶ auto-approved"
  echo "$APPROVED" | sed 's/^/  - /'
fi

# 3. 현재 pending 태스크 정렬 출력
echo ""
echo "================================================================"
echo "  실행 가능 (pending)"
echo "================================================================"
PENDING="$("$CMD" "$QUEUE_FILE" ids-by-status pending 2>/dev/null || true)"
if [[ -z "$PENDING" ]]; then
  echo "  (없음)"
else
  while IFS= read -r tid; do
    [[ -z "$tid" ]] && continue
    title="$("$CMD" "$QUEUE_FILE" field "$tid" title 2>/dev/null || echo '-')"
    role="$( "$CMD" "$QUEUE_FILE" field "$tid" role  2>/dev/null || echo '-')"
    agent="$("$CMD" "$QUEUE_FILE" field "$tid" agent 2>/dev/null || echo '-')"
    echo "  • [$role / $agent] $tid"
    echo "      $title"
  done <<< "$PENDING"
fi

# 4. 진행 중 / 리뷰 / 실패 태스크 요약
echo ""
echo "================================================================"
echo "  상태 요약"
echo "================================================================"
for s in in_progress review failed blocked awaiting_approval; do
  ids="$("$CMD" "$QUEUE_FILE" ids-by-status "$s" 2>/dev/null || true)"
  if [[ -n "$ids" ]]; then
    n=$(echo "$ids" | grep -c .)
    echo "  $s: $n"
    echo "$ids" | sed 's/^/    - /'
  fi
done

echo ""
echo "================================================================"
echo "  다음 단계 — 직접 진행하거나, Claude에게 다음을 요청:"
echo "    \"위 pending 태스크들 멀티에이전트로 처리해줘\""
echo "================================================================"
