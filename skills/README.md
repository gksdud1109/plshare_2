# Skills Registry

이 파일은 **등록부(registry)**다. 실제 skill 내용은 각 하위 폴더의 `SKILL.md`에만 있다.
PA(Playlist Asset) 제품의 역할 시스템(`docs/agent-operating-model.md`)에 맞춰, 고레버리지 skill만 선별해 패키징한다.

---

## 1. 운영 규칙 (고정)

- Skill은 저장소 최상위 `/skills/` 폴더에만 둔다. 다른 위치 중복 금지.
- 모든 skill은 **하나의 폴더 + `SKILL.md` 한 개**로 구성한다. 선택적으로 `templates/`, `checklists/`, `references.md`를 추가할 수 있다.
- 이 `skills/README.md`는 **registry**다. skill 본문을 여기에 쓰지 않는다. 1줄 요약과 링크만.
- 신규 skill 추가:
  1. `/skills/<skill-name>/SKILL.md`를 만든다.
  2. 아래 "2. 활성 skill" 표에 한 행을 추가한다.
  3. `docs/agent-operating-model.md`에서 해당 역할 문서에 skill 이름을 링크한다.
- 각 `SKILL.md`는 다음 섹션을 **반드시** 모두 포함한다: purpose, when to use, when not to use, required inputs, workflow (exact steps), expected outputs, role mapping, handoff notes.
- 외부 저장소에서 가져온 skill은 frontmatter `source` 필드에 원본 URL을 기입한다. 내용은 이 레포의 역할 시스템·문서 구조에 맞게 **반드시 adapt**한다. 원문 복붙 금지.
- 폐기: 행을 지우지 말고 `status`를 `archived`로 바꾸고 사유를 `notes`에 남긴다. 폴더는 `/skills/_archive/` 아래로 이동.

---

## 2. 활성 skill

| name | status | role | purpose (1줄) | 주요 산출물 | source |
|---|---|---|---|---|---|
| [prd-sharpening](./prd-sharpening/SKILL.md) | active | Product Owner | 3단계 loop로 PRD·요구사항·전략 문서의 모호함 제거 | `docs/prd/*` | [doc-coauthoring](https://github.com/anthropics/skills/tree/main/skills/doc-coauthoring) |
| [ux-spec-authoring](./ux-spec-authoring/SKILL.md) | active | Product Designer | aesthetic direction lock → flow → screen spec → copy guide | `docs/ux/*` | [frontend-design](https://github.com/anthropics/skills/tree/main/skills/frontend-design) |
| [frontend-implementation](./frontend-implementation/SKILL.md) | active | Frontend Engineer | UX 스펙을 5-state 커버리지·aesthetic 가드레일 아래 코드로 반영 | `app/*`, `components/*`, `docs/implementation/ui-decision-log.md` | [frontend-design](https://github.com/anthropics/skills/tree/main/skills/frontend-design) |
| [webapp-testing](./webapp-testing/SKILL.md) | active | Frontend + Reviewer/Operator | Playwright로 Import→Log→Export golden path 회귀 방지 | `tests/e2e/*.spec.ts` | [webapp-testing](https://github.com/anthropics/skills/tree/main/skills/webapp-testing) |
| [claude-api-integration](./claude-api-integration/SKILL.md) | active | Backend Engineer | 정규화 fallback·감정 추출·요약에 Claude API를 SDK·캐싱·eval 가드와 함께 통합 | `lib/ai/*`, `tests/ai/*`, `docs/implementation/decision-log.md` | [claude-api](https://github.com/anthropics/skills/tree/main/skills/claude-api) |

---

## 3. 역할별 진입점

`docs/agent-operating-model.md`의 역할 기준. 한 역할에 여러 skill이 매핑될 수 있다.

- **Product Owner** → `prd-sharpening`
- **Product Designer** → `ux-spec-authoring`
- **Frontend Engineer** → `frontend-implementation`, `webapp-testing`
- **Backend Engineer** → `claude-api-integration`
- **Reviewer / Operator** → `webapp-testing` (release gate), `prd-sharpening` (reader test 대행)

---

## 4. 평가했으나 반려한 후보

| candidate | 반려 사유 |
|---|---|
| brand-guidelines | 현재 단계에서 브랜드 VI는 PO-Designer 1회 세션으로 충분. skill 패키징 시 오버헤드가 가치보다 큼. |
| mcp-builder | MVP 범위에 MCP 서버 필요 없음. 추후 B2B 데이터 공급 단계에서 재평가. |
| skill-creator | 이 registry가 동일 역할을 커버. 메타 skill로 별도 패키징 시 중복. |
| docx / pptx / xlsx / pdf | 현재 산출물 전부 `.md`. 외부 문서 포맷 필요 시점에 재평가. |
| algorithmic-art / canvas-design / theme-factory | Aesthetic 방향은 `ux-spec-authoring`의 Step 1에 흡수. 별도 skill 불필요. |
| internal-comms / slack-gif-creator | 내부 커뮤니케이션 루틴이 아직 없음. |

---

## 5. 업데이트 이력

- 2026-04-11: 초기 5개 skill 패키징 (`prd-sharpening`, `ux-spec-authoring`, `frontend-implementation`, `webapp-testing`, `claude-api-integration`).
