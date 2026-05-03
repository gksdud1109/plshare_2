#!/bin/zsh
# 태스크 승인: awaiting_approval → pending
# 사용법:
#   bash .claude/scripts/approve.sh all          # 전체 승인
#   bash .claude/scripts/approve.sh <task-id>    # 개별 승인
set -euo pipefail

QUEUE_FILE="${QUEUE_FILE:-docs/10-operations/tasks/task-queue.yaml}"
CMD="./.claude/scripts/task_queue.rb"

if [[ $# -lt 1 ]]; then
  echo "사용법: $0 <task-id|all>" >&2
  echo ""
  echo "  전체 승인:  $0 all"
  echo "  개별 승인:  $0 <task-id>"
  echo ""
  echo "승인 대기 목록 보기: bash .claude/scripts/propose-decisions.sh"
  exit 1
fi

TARGET="$1"

if [[ "$TARGET" == "all" ]]; then
  APPROVED=("${(@f)$("$CMD" "$QUEUE_FILE" approve-all)}")
  if [[ ${#APPROVED[@]} -eq 0 || -z "${APPROVED[1]:-}" ]]; then
    echo "승인 대기 태스크 없음"
  else
    for ID in "${APPROVED[@]}"; do
      [[ -z "$ID" ]] && continue
      echo "승인됨: $ID"
    done
    echo ""
    echo "실행: bash .claude/scripts/run-orchestrator.sh"
  fi
else
  "$CMD" "$QUEUE_FILE" approve "$TARGET"
  echo ""
  echo "실행: bash .claude/scripts/run-orchestrator.sh"
fi
