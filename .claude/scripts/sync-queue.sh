#!/bin/zsh
set -euo pipefail

QUEUE_FILE="${1:-docs/10-operations/tasks/task-queue.yaml}"
export QUEUE_FILE

source ./.claude/scripts/task-lib.sh
require_queue_cmd

UNBLOCKED=("${(@f)$(./.claude/scripts/task_queue.rb "$QUEUE_FILE" unblock-ready)}")
if [[ ${#UNBLOCKED[@]} -eq 0 || -z "${UNBLOCKED[1]:-}" ]]; then
  echo "no blocked tasks unblocked"
else
  printf '%s\n' "${UNBLOCKED[@]}"
fi
