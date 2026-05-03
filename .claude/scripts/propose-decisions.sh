#!/bin/zsh
# 승인 대기 중인 태스크를 의사결정 브리핑으로 출력
set -euo pipefail

QUEUE_FILE="${1:-docs/10-operations/tasks/task-queue.yaml}"
export QUEUE_FILE

source ./.claude/scripts/task-lib.sh
require_queue_cmd

AWAITING=("${(@f)$(task_ids_by_status "awaiting_approval")}")

if [[ ${#AWAITING[@]} -eq 0 || -z "${AWAITING[1]:-}" ]]; then
  echo "승인 대기 태스크 없음. 현재 상태:"
  echo ""
  for STATUS in pending in_progress review blocked done failed; do
    IDS=("${(@f)$(task_ids_by_status "$STATUS")}")
    COUNT=0
    for ID in "${IDS[@]}"; do [[ -n "$ID" ]] && COUNT=$((COUNT+1)); done
    [[ $COUNT -gt 0 ]] && echo "  $STATUS: $COUNT"
  done
  exit 0
fi

echo "================================================================"
echo "  의사결정 브리핑 — $(date '+%Y-%m-%d %H:%M')"
echo "================================================================"
echo ""
echo "승인 대기: ${#AWAITING[@]}개 태스크"
echo ""

IDX=0
for TASK_ID in "${AWAITING[@]}"; do
  [[ -z "$TASK_ID" ]] && continue
  IDX=$((IDX+1))

  TITLE="$(task_field "$TASK_ID" "title")"
  ROLE="$(task_field "$TASK_ID" "role")"
  AGENT="$(task_field "$TASK_ID" "agent")"
  PRIORITY="$(task_field "$TASK_ID" "priority")"
  BRIEFING="$(task_field "$TASK_ID" "briefing_path")"
  DEPS="$(task_field "$TASK_ID" "depends_on")"

  echo "----------------------------------------------------------------"
  printf "  [%s] #%d  %s\n" "$PRIORITY" "$IDX" "$TASK_ID"
  echo "----------------------------------------------------------------"
  echo "  제목:   $TITLE"
  echo "  담당:   $ROLE"
  echo "  에이전트: $AGENT"
  [[ "$DEPS" != "null" && -n "$DEPS" ]] && echo "  의존:   $DEPS"
  echo ""

  if [[ -f "$BRIEFING" ]]; then
    # Done Criteria 섹션만 추출
    IN_CRITERIA=0
    while IFS= read -r LINE; do
      if [[ "$LINE" =~ "^## Done Criteria" ]]; then
        IN_CRITERIA=1
        echo "  완료 기준:"
        continue
      fi
      if [[ $IN_CRITERIA -eq 1 ]]; then
        [[ "$LINE" =~ "^## " ]] && break
        [[ -n "$LINE" ]] && echo "    $LINE"
      fi
    done < "$BRIEFING"
    echo ""
    echo "  브리핑 전문: $BRIEFING"
  fi
  echo ""
done

echo "================================================================"
echo ""
echo "  승인 명령:"
echo ""
echo "    전체 승인   →  bash .claude/scripts/approve.sh all"
for TASK_ID in "${AWAITING[@]}"; do
  [[ -z "$TASK_ID" ]] && continue
  echo "    $TASK_ID  →  bash .claude/scripts/approve.sh $TASK_ID"
done
echo ""
echo "  승인 후 실행: bash .claude/scripts/run-orchestrator.sh"
echo "================================================================"
