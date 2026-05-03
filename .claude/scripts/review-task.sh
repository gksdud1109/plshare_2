#!/bin/zsh
set -euo pipefail

QUEUE_FILE="${1:-docs/10-operations/tasks/task-queue.yaml}"
TASK_ID_FILTER="${2:-}"
export QUEUE_FILE

source ./.claude/scripts/task-lib.sh
require_queue_cmd

has_error_signal() {
  local log_path="$1"
  [[ ! -f "$log_path" ]] && return 0
  [[ ! -s "$log_path" ]] && return 1
  if rg -n "error|exception|traceback|failed|fatal" "$log_path" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

all_artifacts_present() {
  local task_id="$1"
  local artifacts
  artifacts=("${(@f)$(task_field "$task_id" "artifact_paths")}")
  local artifact
  for artifact in "${artifacts[@]}"; do
    [[ -z "$artifact" || "$artifact" == "null" ]] && continue
    if [[ ! -f "$artifact" ]]; then
      return 1
    fi
    if [[ ! -s "$artifact" ]]; then
      return 1
    fi
  done
  return 0
}

# started_at 이후 artifact가 실제로 변경됐는지 git 또는 mtime으로 검증
artifacts_actually_changed() {
  local task_id="$1"
  local artifacts started_at
  artifacts=("${(@f)$(task_field "$task_id" "artifact_paths")}")
  started_at="$(task_field "$task_id" "started_at")"

  # started_at이 없으면 검증 스킵 (신규 파일 생성 태스크)
  if [[ -z "$started_at" || "$started_at" == "null" ]]; then
    return 0
  fi

  # git이 있으면 git diff 기준으로 확인
  if git rev-parse --git-dir >/dev/null 2>&1; then
    local any_changed=0
    for artifact in "${artifacts[@]}"; do
      [[ -z "$artifact" || "$artifact" == "null" ]] && continue
      # 신규 파일(untracked) 또는 수정된 파일이면 changed
      if git status --porcelain "$artifact" 2>/dev/null | grep -q .; then
        any_changed=1
        break
      fi
    done
    [[ "$any_changed" -eq 1 ]] && return 0

    # 모든 artifact가 git 기준으로 unchanged면 실패
    return 1
  fi

  # git 없으면 mtime 기반 확인 (started_at 이후 수정 여부)
  local started_epoch
  started_epoch="$(date -j -f '%Y-%m-%dT%H:%M:%S%z' "$started_at" '+%s' 2>/dev/null || echo 0)"
  for artifact in "${artifacts[@]}"; do
    [[ -z "$artifact" || "$artifact" == "null" ]] && continue
    local file_epoch
    file_epoch="$(stat -f '%m' "$artifact" 2>/dev/null || echo 0)"
    if [[ "$file_epoch" -gt "$started_epoch" ]]; then
      return 0
    fi
  done
  return 1
}

mark_done() {
  local task_id="$1"
  local role next_role title artifacts
  role="$(task_field "$task_id" "role")"
  next_role="$(task_field "$task_id" "next_role")"
  title="$(task_field "$task_id" "title")"
  artifacts="$(task_field "$task_id" "artifact_paths" | paste -sd ', ' -)"
  "./.claude/scripts/task_queue.rb" "$QUEUE_FILE" set-status "$task_id" "done" "__NULL__"
  append_decision_log "Reviewer / Operator" "Claude" "Auto-approved task ${task_id} (${title})" "Artifacts exist, are non-empty, and log has no error signal; next role=${next_role}" "$artifacts"
  echo "done task=$task_id"
}

mark_failed() {
  local task_id="$1"
  local reason="$2"
  "./.claude/scripts/task_queue.rb" "$QUEUE_FILE" set-status "$task_id" "failed" "$reason"
  echo "failed task=$task_id reason=$reason"
}

if [[ -n "$TASK_ID_FILTER" ]]; then
  REVIEW_IDS=("$TASK_ID_FILTER")
else
  REVIEW_IDS=("${(@f)$(task_ids_by_status "review")}")
fi

if [[ ${#REVIEW_IDS[@]} -eq 0 ]]; then
  echo "no review tasks"
  exit 0
fi

for TASK_ID in "${REVIEW_IDS[@]}"; do
  [[ -z "$TASK_ID" ]] && continue
  LOG_PATH="$(task_field "$TASK_ID" "log_path")"

  if ! all_artifacts_present "$TASK_ID"; then
    mark_failed "$TASK_ID" "artifact missing or empty"
    continue
  fi

  if has_error_signal "$LOG_PATH"; then
    mark_failed "$TASK_ID" "error signal found in log: $LOG_PATH"
    continue
  fi

  # 실제 변경이 없으면 실패 처리 (파일 존재만으로 done 처리 방지)
  if ! artifacts_actually_changed "$TASK_ID"; then
    mark_failed "$TASK_ID" "artifacts exist but no changes detected (git diff or mtime)"
    continue
  fi

  mark_done "$TASK_ID"
done
