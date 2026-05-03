#!/bin/zsh
set -euo pipefail

QUEUE_FILE="${QUEUE_FILE:-docs/10-operations/tasks/task-queue.yaml}"
QUEUE_CMD="./.claude/scripts/task_queue.rb"

require_queue_cmd() {
  if [[ ! -x "$QUEUE_CMD" ]]; then
    echo "task queue helper is missing or not executable: $QUEUE_CMD" >&2
    exit 2
  fi
}

now_iso() {
  date '+%Y-%m-%dT%H:%M:%S%z'
}

task_ids_by_status() {
  local task_status="$1"
  "$QUEUE_CMD" "$QUEUE_FILE" ids-by-status "$task_status"
}

task_field() {
  local task_id="$1"
  local field_name="$2"
  "$QUEUE_CMD" "$QUEUE_FILE" field "$task_id" "$field_name"
}

append_decision_log() {
  local role="$1"
  local model="$2"
  local decision="$3"
  local rationale="$4"
  local affected="$5"
  local log_file="docs/20-product/delivery/implementation/decision-log.md"
  local line
  line="- \`$(date '+%Y-%m-%d') | ${role} | ${model} | ${decision} | ${rationale} | ${affected}\`"
  if [[ ! -f "$log_file" ]]; then
    echo "# Decision Log" > "$log_file"
    echo >> "$log_file"
  fi
  printf '%s\n' "$line" >> "$log_file"
}

all_dependencies_done() {
  local task_id="$1"
  [[ "$("$QUEUE_CMD" "$QUEUE_FILE" deps-done "$task_id")" == "true" ]]
}

next_ready_pending_id() {
  "$QUEUE_CMD" "$QUEUE_FILE" next-ready-pending
}
