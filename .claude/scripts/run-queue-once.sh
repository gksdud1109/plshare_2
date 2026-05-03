#!/bin/zsh
set -euo pipefail

QUEUE_FILE="${1:-docs/10-operations/tasks/task-queue.yaml}"
export QUEUE_FILE
source ./.claude/scripts/task-lib.sh
require_queue_cmd

TASK_ID="$(next_ready_pending_id 2>/dev/null || true)"

if [[ -z "$TASK_ID" ]]; then
  echo "no ready pending tasks"
  exit 0
fi

AGENT="$(task_field "$TASK_ID" "agent")"
BRIEFING_PATH="$(task_field "$TASK_ID" "briefing_path")"
LOG_PATH="$(task_field "$TASK_ID" "log_path")"
ARTIFACT_PATH="$(task_field "$TASK_ID" "artifact_paths" | head -n 1)"

if [[ ! -f "$BRIEFING_PATH" ]]; then
  echo "briefing missing for task $TASK_ID: $BRIEFING_PATH" >&2
  exit 2
fi

"./.claude/scripts/task_queue.rb" "$QUEUE_FILE" mark-in-progress "$TASK_ID"

if ./.claude/scripts/dispatch.sh "$AGENT" "$BRIEFING_PATH" "$ARTIFACT_PATH" "$LOG_PATH"; then
  "./.claude/scripts/task_queue.rb" "$QUEUE_FILE" mark-finished "$TASK_ID" "review" "0"
else
  EXIT_CODE="$?"
  "./.claude/scripts/task_queue.rb" "$QUEUE_FILE" mark-finished "$TASK_ID" "failed" "$EXIT_CODE" "see $LOG_PATH"
fi

echo "processed task=$TASK_ID"
