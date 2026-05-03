#!/bin/zsh
# 에이전트 워커 루프 — 각 CLI 터미널에서 직접 실행
#
# 사용법:
#   zsh .claude/scripts/worker.sh gemini-cli
#   zsh .claude/scripts/worker.sh codex-cli
#   zsh .claude/scripts/worker.sh claude-code
#
# 동작: 큐를 주기적으로 폴링하여 자기 역할의 태스크를 발견하면
#       CLI를 이 터미널에서 직접 실행하고 결과를 실시간으로 출력

set -uo pipefail

AGENT="${1:?사용법: $0 <agent>  예: $0 gemini-cli}"
QUEUE_FILE="${QUEUE_FILE:-docs/10-operations/tasks/task-queue.yaml}"
POLL_INTERVAL="${POLL_INTERVAL:-8}"
CMD="./.claude/scripts/task_queue.rb"

export QUEUE_FILE
source ./.claude/scripts/task-lib.sh
require_queue_cmd

mkdir -p .claude/logs

echo ""
echo "╔══════════════════════════════════════╗"
echo "║  Worker 시작: $AGENT"
printf "║  큐: %-33s║\n" "$QUEUE_FILE"
printf "║  폴링: %ds  |  중단: Ctrl+C         ║\n" "$POLL_INTERVAL"
echo "╚══════════════════════════════════════╝"
echo ""

run_task() {
  local task_id="$1"
  local briefing artifact log_path title exit_code

  title="$(task_field "$task_id" "title")"
  briefing="$(task_field "$task_id" "briefing_path")"
  log_path="$(task_field "$task_id" "log_path")"
  artifact="$(task_field "$task_id" "artifact_paths" | head -n 1)"

  echo "┌──────────────────────────────────────"
  echo "│ 태스크: $task_id"
  echo "│ 제목:   $title"
  echo "│ 시각:   $(date '+%Y-%m-%d %H:%M:%S')"
  echo "└──────────────────────────────────────"

  if [[ ! -f "$briefing" ]]; then
    echo "[오류] 브리핑 파일 없음: $briefing"
    "$CMD" "$QUEUE_FILE" mark-finished "$task_id" "failed" "1" "briefing missing: $briefing"
    return
  fi

  mkdir -p "$(dirname "$log_path")"
  [[ -n "$artifact" && "$artifact" != "null" ]] && mkdir -p "$(dirname "$artifact")" 2>/dev/null || true

  echo ""
  echo "── 브리핑 ──────────────────────────────"
  cat "$briefing"
  echo ""
  echo "── 실행 ────────────────────────────────"
  echo ""

  "$CMD" "$QUEUE_FILE" mark-in-progress "$task_id"

  exit_code=0
  case "$AGENT" in
    gemini-cli)
      # stdout → 터미널 표시 + artifact 저장 + log 저장
      if [[ -n "$artifact" && "$artifact" != "null" ]]; then
        gemini -p "$(cat "$briefing")" 2>&1 | tee "$artifact" "$log_path" || exit_code="${pipestatus[1]}"
      else
        gemini -p "$(cat "$briefing")" 2>&1 | tee "$log_path" || exit_code="${pipestatus[1]}"
      fi
      ;;
    codex-cli)
      # 파일 직접 수정 + 터미널 출력 + log 저장
      codex exec --full-auto "$(cat "$briefing")" 2>&1 | tee "$log_path" || exit_code="${pipestatus[1]}"
      ;;
    claude-code)
      # 파일 직접 수정 + 터미널 출력 + log 저장
      claude -p "$(cat "$briefing")" \
        --allowedTools "Read,Edit,Write,Bash(git status:*),Bash(git diff:*)" \
        --dangerously-skip-permissions \
        2>&1 | tee "$log_path" || exit_code="${pipestatus[1]}"
      ;;
    *)
      echo "[오류] 지원하지 않는 에이전트: $AGENT"
      exit_code=2
      ;;
  esac

  echo ""
  if [[ "$exit_code" -eq 0 ]]; then
    "$CMD" "$QUEUE_FILE" mark-finished "$task_id" "review" "0"
    echo "✓ 완료 → review 대기 중 (오케스트레이터가 검토)"
  else
    "$CMD" "$QUEUE_FILE" mark-finished "$task_id" "failed" "$exit_code" "see $log_path"
    echo "✗ 실패 (exit=$exit_code) | 로그: $log_path"
  fi
  echo ""
}

# 메인 폴링 루프
while true; do
  TASK_ID="$("$CMD" "$QUEUE_FILE" next-pending-for-agent "$AGENT" 2>/dev/null || true)"

  if [[ -n "$TASK_ID" ]]; then
    run_task "$TASK_ID"
  else
    printf "\r[%s] 대기 중... " "$(date '+%H:%M:%S')"
    sleep "$POLL_INTERVAL"
  fi
done
