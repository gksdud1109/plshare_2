# Git Workflow Rules

이 문서는 본 저장소에서 **사람 + AI 에이전트**가 섞여 병렬로 작업할 때 반드시 따라야 할 git 운영 규칙이다.
과거에 실제로 발생한 사고들을 근거로 작성되었다. **규칙을 벗어나야 할 것 같으면 먼저 멈추고 사용자에게 확인**한다.

---

## 1. 브랜치 네이밍 & 격리

모든 작업은 **독립 브랜치**에서 한다. `main`에는 절대 직접 커밋하지 않는다.

| 작업 유형 | 브랜치 prefix | 예시 |
|---|---|---|
| Frontend 기능 | `feat/fe/` | `feat/fe/initial-scaffold` |
| Backend 기능 | `feat/be/` | `feat/be/domain-implementation` |
| 운영/문서/프로세스 | `ops/` | `ops/git-workflow-rules` |
| 버그 수정 | `fix/<area>/` | `fix/be/import-tx-leak` |
| 릴리즈 준비 | `release/` | `release/v0.3.0` |

규칙:
- **브랜치 prefix와 작업 영역은 반드시 일치**해야 한다. `feat/fe` 브랜치에 `backend/` 코드를 커밋하는 것은 **금지**.
- 한 브랜치는 한 작업 단위만 담는다. 여러 작업을 섞어 넣지 않는다.
- 이미 존재하는 브랜치명을 재사용하지 않는다. `git branch -a`로 먼저 확인한다.

---

## 2. 세션 시작 체크리스트 (Pre-flight)

터미널에 들어가면 **무조건 먼저 실행**한다:

```bash
git branch --show-current        # 내가 어느 브랜치에 있는지
git status --short               # 더러운 파일이 있는가
git log --oneline -5             # 최근 히스토리
git stash list                   # 미해결 stash가 있는가
```

네 항목 중 하나라도 예상과 다르면 **작업 시작 전에 먼저 정리**한다. 더러운 working tree 위에 새 작업을 쌓지 않는다.

---

## 3. 브랜치 전환 시 규칙

병렬 작업 중 가장 자주 사고가 나는 지점이다. 다음 순서를 반드시 지킨다:

1. 현재 WIP가 있으면 **먼저 커밋하거나 stash**한다.
   - 커밋이 더 안전하다. 기록이 남는다.
   - stash를 쓸 때는 **반드시 메시지**와 **`-u` 옵션**을 붙인다: `git stash push -u -m "be-wip: asset refactor"`
2. `git status --short`로 working tree가 깨끗한지 확인한다.
3. `git checkout <target-branch>` 로 이동한다.
4. 이동한 다음 `git status`를 한 번 더 확인한다. 예상 못한 변경이 남아 있으면 의심한다.

**절대 하지 않는 것**:
- `git checkout`을 더러운 working tree에서 실행하고 git이 암묵적으로 변경을 가져가게 두는 것.
- stash 메시지 없이 `git stash` 만 치고 넘어가는 것 (나중에 뭐가 들어있는지 못 찾는다).

---

## 4. 커밋 규칙

- **커밋 전에 항상 `git branch --show-current`를 눈으로 확인**한다. 브랜치명과 변경 내용이 일치하는지 검사한다.
- 커밋 메시지는 `<type>(<scope>): <summary>` 형식을 따른다:
  - `type`: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `ops`
  - `scope`: `fe`, `be`, `ops`, `docs`, 또는 더 좁은 영역
  - 예: `feat(be): add idempotency key store with 24h TTL`
- 커밋 본문은 **WHY**를 적는다. WHAT은 diff를 보면 된다.
- 관련 없는 변경은 절대 한 커밋에 섞지 않는다. `git add <specific files>` 를 쓰고 `git add -A` 는 피한다.
- **절대 `main`에 직접 commit하거나 push하지 않는다.**
- 사용자의 명시적 요청 없이 `--amend`나 `git rebase -i`를 하지 않는다.

---

## 5. Stash 사용 규칙

- 반드시 `-m` 메시지를 붙인다. 의미 없는 `WIP on ...`는 금지.
- untracked 파일까지 포함하려면 **반드시 `-u`** 를 붙인다. 안 그러면 나중에 파일이 사라진 것처럼 보인다.
- stash를 pop 하기 전에 **반드시 `git stash show -u --name-only stash@{0}`** 로 내용물을 먼저 확인한다.
- 같은 브랜치에서 쌓인 stash만 그 브랜치에 pop한다. 다른 브랜치에서 만든 stash는 `git checkout <stash-ref> -- <path>`로 **필요한 파일만 선택적으로** 꺼낸다.
- 오래된 stash는 정리한다. `git stash list` 에 5개 이상 쌓이면 뭔가 잘못된 것이다.

---

## 6. 파괴적 명령 금지

다음 명령들은 **사용자가 명시적으로 요청하지 않는 한 실행하지 않는다**:

- `git reset --hard`
- `git push --force`, `git push --force-with-lease`
- `git branch -D` (브랜치 강제 삭제)
- `git checkout .` 또는 `git restore .`
- `git clean -fd`
- `git rebase` (미리 조율되지 않았다면)

필요해 보이면 먼저 사용자에게 상황을 설명하고 승인을 받는다. reflog로 복구 가능하더라도 **사용자의 명시적 기록**을 남기는 것이 원칙이다.

---

## 7. 병렬 에이전트 운영 시 추가 규칙

여러 에이전트가 동시에 작업할 때 (FE와 BE 병렬 개발 등):

- **에이전트는 자기 prefix 영역 밖의 파일을 수정하지 않는다.** FE 에이전트는 `/backend`를, BE 에이전트는 `/frontend`를 건드리지 않는다. 운영 문서(`/docs/10-operations/`)도 자기 작업과 직접 관련 없으면 손대지 않는다.
- 다른 에이전트의 브랜치에 커밋하지 않는다. 자기 작업은 자기 브랜치에만.
- 브랜치를 만들기 전에 `git branch -a`로 이름 충돌을 확인한다.
- 다른 에이전트의 미완성 WIP를 건드려야 할 것 같으면 **먼저 멈추고 사용자에게 handoff 상태를 확인**한다.

---

## 8. `.gitignore` 관리

- **루트 `.gitignore`가 source of truth**이다. 서브디렉터리의 `.gitignore`(예: `frontend/.gitignore`)는 보조 수단일 뿐이다.
- 서브디렉터리가 untracked 상태일 땐 그 안의 `.gitignore`는 **상속되지 않는다**. 그래서 루트에서 `frontend/node_modules/` 같은 경로를 반드시 직접 명시해야 한다.
- `.env`는 기본 ignore, `.env.example`는 명시적으로 un-ignore: `!.env.example`.
- 빌드 산출물(`build/`, `.next/`, `node_modules/`, `.gradle/`)은 언제나 ignore.

---

## 9. 자주 나는 사고 유형 (학습용)

실제로 본 저장소에서 발생했거나, 같은 패턴으로 예상되는 사고들:

| 사고 | 원인 | 예방책 |
|---|---|---|
| **BE 작업이 FE 브랜치에 커밋됨** | 브랜치 전환 없이 작업, 커밋 전 브랜치 확인 누락 | §2 체크리스트, §4 "커밋 전 브랜치 확인" |
| **20,000+ 파일이 untracked로 잡힘** | 루트 `.gitignore`가 없어 FE 브랜치에 없는 `frontend/` 디렉터리 내부가 전부 노출됨 | §8 루트 `.gitignore` 관리 |
| **작업 내용이 stash에서 사라짐** | `-u` 옵션 없이 stash해서 untracked 파일이 누락됨 | §5 Stash 규칙 |
| **두 에이전트가 같은 브랜치에 동시 커밋** | 브랜치 네이밍 충돌, pre-flight 누락 | §1, §2 |
| **파괴적 reset으로 작업 소실** | `git reset --hard`를 확인 없이 실행 | §6 파괴적 명령 금지 |

---

## 10. 문제가 생겼을 때

- 뭔가 꼬였으면 **먼저 멈춘다**. 추가 명령으로 수습하려다 더 꼬이는 경우가 많다.
- `git reflog` 는 대부분의 사고를 복구할 수 있게 해준다. 파괴적 명령을 쓰기 전 reflog를 먼저 확인한다.
- 판단이 서지 않으면 **사용자에게 상황을 설명하고 복구안을 제안**한다. 혼자 즉흥으로 복구하지 않는다.
