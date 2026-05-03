#!/bin/zsh
set -euo pipefail

QUEUE_FILE="${1:-docs/10-operations/tasks/task-queue.yaml}"
export QUEUE_FILE

source ./.claude/scripts/task-lib.sh
require_queue_cmd

mkdir -p .claude/logs

ITERATIONS=0
MAX_ITERATIONS=20

while [[ "$ITERATIONS" -lt "$MAX_ITERATIONS" ]]; do
  ITERATIONS=$((ITERATIONS + 1))
  PROGRESS=0

  # blocked → awaiting_approval/pending 전환 감지
  BEFORE_BLOCKED=("${(@f)$(task_ids_by_status "blocked")}")
  ./.claude/scripts/sync-queue.sh "$QUEUE_FILE" >/dev/null || true
  AFTER_BLOCKED=("${(@f)$(task_ids_by_status "blocked")}")
  # 빈 배열 처리: 첫 요소가 빈 문자열이면 실제 0개
  BEFORE_COUNT=0
  AFTER_COUNT=0
  for id in "${BEFORE_BLOCKED[@]}"; do [[ -n "$id" ]] && BEFORE_COUNT=$((BEFORE_COUNT+1)); done
  for id in "${AFTER_BLOCKED[@]}"; do  [[ -n "$id" ]] && AFTER_COUNT=$((AFTER_COUNT+1)); done
  if [[ "$BEFORE_COUNT" -ne "$AFTER_COUNT" ]]; then
    PROGRESS=1
  fi

  # review 상태 태스크 처리
  _review_raw="$(task_ids_by_status "review")"
  if [[ -n "$_review_raw" ]]; then
    REVIEW_IDS=("${(@f)$_review_raw}")
    ./.claude/scripts/review-task.sh "$QUEUE_FILE" >/dev/null
    PROGRESS=1
    continue
  fi

  # claude-code 태스크: 서브프로세스 대신 사람(오케스트레이터)에게 알림
  _cc_task="$(./.claude/scripts/task_queue.rb "$QUEUE_FILE" next-pending-for-agent "claude-code" 2>/dev/null || true)"
  if [[ -n "$_cc_task" ]]; then
    echo ""
    echo ">>> claude-code 태스크 대기: $_cc_task"
    echo "    worker.sh claude-code 를 실행하거나, Claude가 직접 처리하세요."
    echo ""
    # 자동 dispatch 하지 않음 — worker 또는 수동 처리 대기
    PROGRESS=1
    break
  fi

  # gemini/codex 태스크: worker.sh 가 처리 (orchestrator는 dispatch 안 함)
  _pending_raw="$(next_ready_pending_id 2>/dev/null || true)"
  if [[ -n "$_pending_raw" ]]; then
    echo ">>> pending 태스크 감지: $_pending_raw (worker가 처리 예정)"
    PROGRESS=1
  fi

  if [[ "$PROGRESS" -eq 0 ]]; then
    break
  fi
done

echo "orchestrator complete after $ITERATIONS iteration(s)"
