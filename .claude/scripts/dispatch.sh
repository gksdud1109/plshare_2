#!/bin/zsh
set -euo pipefail

if [[ $# -lt 4 ]]; then
  echo "usage: $0 <agent> <briefing_file> <artifact_path> <log_path>" >&2
  exit 2
fi

AGENT="$1"
BRIEFING_FILE="$2"
ARTIFACT_PATH="$3"
LOG_PATH="$4"

if [[ ! -f "$BRIEFING_FILE" ]]; then
  echo "briefing file not found: $BRIEFING_FILE" >&2
  exit 2
fi

mkdir -p "$(dirname "$ARTIFACT_PATH")"
mkdir -p "$(dirname "$LOG_PATH")"

TMP_ARTIFACT="$(mktemp "${TMPDIR:-/tmp}/artifact.XXXXXX")"
TMP_LOG="$(mktemp "${TMPDIR:-/tmp}/log.XXXXXX")"

cleanup() {
  rm -f "$TMP_ARTIFACT" "$TMP_LOG"
}
trap cleanup EXIT

EXIT_CODE=0
# stdout: gemini writes output to stdout → captured as artifact
# workspace: codex/claude write files directly → stdout goes to log only, artifact checked separately
AGENT_MODE="stdout"

case "$AGENT" in
  gemini-cli)
    AGENT_MODE="stdout"
    if ! gemini -p "$(cat "$BRIEFING_FILE")" >"$TMP_ARTIFACT" 2>"$TMP_LOG"; then
      EXIT_CODE="$?"
    fi
    ;;
  codex-cli)
    AGENT_MODE="workspace"
    # codex exec: 비대화형 실행, --full-auto: 자동 승인 + 샌드박스
    if ! codex exec --full-auto "$(cat "$BRIEFING_FILE")" >"$TMP_LOG" 2>&1; then
      EXIT_CODE="$?"
    fi
    ;;
  claude-code)
    AGENT_MODE="workspace"
    # --allowedTools: 파일 편집 허용 / --dangerously-skip-permissions: 실행 중 확인 프롬프트 없이 자동 진행
    if ! claude -p "$(cat "$BRIEFING_FILE")" \
        --allowedTools "Read,Edit,Write,Bash(git status:*),Bash(git diff:*)" \
        --dangerously-skip-permissions \
        >"$TMP_LOG" 2>&1; then
      EXIT_CODE="$?"
    fi
    ;;
  *)
    echo "unsupported agent: $AGENT" >&2
    exit 2
    ;;
esac

mv "$TMP_LOG" "$LOG_PATH"
# stdout 모드만 artifact를 TMP에서 이동. workspace 모드는 에이전트가 직접 파일을 씀
if [[ "$AGENT_MODE" == "stdout" && -s "$TMP_ARTIFACT" ]]; then
  mv "$TMP_ARTIFACT" "$ARTIFACT_PATH"
fi
echo "artifact=$ARTIFACT_PATH"
echo "log=$LOG_PATH"
exit "$EXIT_CODE"
