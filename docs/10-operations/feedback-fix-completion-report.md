# 피드백 수정 완료 보고

> 기준 계획: `docs/10-operations/feedback-fix-plan.md`  
> 작업일: 2026-06-15  
> 작업 브랜치: `feature/gift-value-fields`

## 1. 완료 요약

계획에서 코드로 확정할 수 있는 PR1~PR8 범위를 구현했다. 제품 정책 결정이 필요한 프로필 공개 목록, DM 선물 수신자 모델, YouTube 검색/import 확장은 구현하지 않았다.

| 항목 | 상태 | 결과 |
|---|---|---|
| item1 랭킹/피드 403 | 완료 | 사적 필드를 제외한 `GET /api/public/assets/{id}` 추가, 소유자 조회 401/403 시 공개 조회로 전환 |
| item2 DM 선물 | 보류 | 수신자 모델 결정 필요 |
| item3 텍스트 포스트 | 조치 없음 | 계획대로 정상 동작 유지 |
| item4 용어/프로필 | 부분 완료 | 관리 용어를 `플레이리스트`로 통일, 정책 미확정 프로필 플레이리스트 탭은 숨김 |
| item5 YouTube 검색/import | 보류 | OAuth·쿼터·제품 범위 결정 필요 |
| item6 제작/선물 분리 | 완료 | 생성 직후 강제 선물 이동 제거, `선물하기`·`내 플레이리스트 보기`·`계속 만들기` 분기 제공 |
| item7 MOOD_VIDEO 미배선 | 완료 | 공용 `MoodVideoPlayer`를 상세·공유·언박싱에 연결, 카드/피드 포맷 표시, export 거짓 성공 차단 |
| item8 선물 선택 미리보기 | 1차 완료 | 포맷 배지와 상위 3곡 또는 영상 채널/수록곡 미리보기 표시 |
| item9 compose 멱등성 | 완료 | 소유자별 `X-Idempotency-Key` 저장 및 유일 제약 추가, 재요청 시 기존 결과 반환 |
| item10 import 상태 계약 | 완료 | `RUNNING`을 `matching`으로 변환하고 구버전 `running`도 수용, 120초 폴링 타임아웃과 라이브러리 이동 제공 |

## 2. 주요 구현

- 공개 상세 DTO에서 `diaryText`, `photoUrls`, `shareToken`을 제외해 랭킹/피드 공개 읽기와 소유자 편집 계약을 분리했다.
- `MOOD_VIDEO` 또는 트랙이 없는 플레이리스트의 export 요청을 `VALIDATION_FAILED`로 거부한다.
- 선물에 참조된 플레이리스트 삭제는 `CONFLICT`로 거부하고, 미참조 플레이리스트만 삭제한다.
- `AssetSummary` 계약에 `assetKind`, 감정 정보, 상위 트랙 미리보기, 영상 메타데이터를 연결했다.
- 언박싱에 있던 검증된 YouTube 임베드 블록을 `MoodVideoPlayer`로 추출해 단일 구현으로 재사용한다.
- 구버전 백엔드와 프론트가 혼재할 때 `previewTracks` 누락으로 선물 화면이 중단되지 않도록 하위 호환 처리했다.

DB 변경:

- `V17__asset_compose_idempotency.sql`
- `assets.compose_idempotency_key`
- `(owner_id, compose_idempotency_key)` 유일 제약

## 3. 검증 결과

자동 검증:

- `backend ./gradlew test` 성공
- `frontend npm run build` 성공
- `frontend npm run lint` 오류 0건, 기존 경고 14건
- `git diff --check` 성공

실제 API 검증:

- 동일 compose 멱등키 2회 요청이 동일 Asset ID 반환
- 공개 상세 `200`, 사적 필드 미포함 확인
- MOOD_VIDEO export `400 VALIDATION_FAILED`
- 미참조 플레이리스트 삭제 `200`
- 선물 참조 플레이리스트 삭제 `409 CONFLICT`

브라우저 검증:

- 생성 완료 후 세 갈래 CTA 노출 확인
- MOOD_VIDEO 상세에서 iframe 재생과 export 불가 안내 확인
- 선물 선택기에서 포맷/상위 트랙 미리보기 확인
- 랭킹의 타인 플레이리스트 클릭 시 공개 상세로 정상 전환 확인
- 공개 상세에서 편집·공유·삭제 컨트롤이 노출되지 않음을 확인

## 4. 후속 결정

다음 항목은 PO 결정 후 별도 작업으로 진행해야 한다.

1. 프로필을 공개 큐레이션 공간으로 사용할지와 공개 플레이리스트 기준
2. DM 선물의 실제 수신자를 `handle`로 지정할지
3. YouTube 검색 기반 곡 추가와 YouTube Music import의 OAuth·쿼터 범위
4. 선물 선택기의 전체 트랙 lazy accordion 2차 확장 여부

기존 `.claude/` 스크립트와 루트 평가 문서 등 작업 전부터 존재한 변경사항은 수정하거나 되돌리지 않았다.
