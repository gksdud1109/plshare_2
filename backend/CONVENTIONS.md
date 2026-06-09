# Backend Conventions

`polyenm_pan` 백엔드의 성숙한 컨벤션을 plshare2에 단계적으로 차용한다.
이 문서는 합의된 규칙과 도입 단계를 기록한다.

## 차용 출처
- `~/Development/polyenm_pan/polyenm-pan` — 도메인형 패키지 구조 + `global/` 횡단 레이어, 표준 응답/예외, BaseEntity 감사 등.

## 단계 (phased adoption)

### Tier 1 — 내부 정돈 (FE/E2E 무영향) ✅ 도입됨
- **표준 예외 처리**: `global/exception/`
  - `ErrorCode` (enum): 클라이언트 분기용 **안정 코드**. 이름 변경 금지(추가만).
  - `ApiException(code, message)`: 서비스/컨트롤러가 던지는 도메인 예외. raw `IllegalState`/`NoSuchElement`/`error()` 대신 이것을 쓴다.
  - `ErrorResponse(code, message, details?)`: 표준 오류 바디.
  - `GlobalExceptionHandler` (`@RestControllerAdvice`): ApiException/검증/NoResource/미처리 예외를 일관 매핑. 4xx=WARN, 5xx=ERROR 로깅.
  - 규칙: **요청 경로에서 raw 예외를 던지지 않는다.** 없으면 `ApiException(NOT_FOUND, …)`, 외부 어댑터 실패는 `UPSTREAM_ERROR`, 잘못된 입력은 `VALIDATION_FAILED`, 상태 충돌은 `CONFLICT`.
  - ⚠️ 성공 응답 형태는 (Tier 3 전까지) **그대로** 둔다 — 오류 응답만 표준화. (FE/E2E가 raw 성공 바디를 소비 중)
- **트랜잭션 경계**: 조회 핸들러/서비스 메서드는 `@Transactional(readOnly = true)`.

### Tier 2 — 구조 재편 (예정)
- **package-by-feature**: `domain/<feature>/{controller, service, repository, model, dto}` + `global/{exception, jpa, web, config, security, response, openapi, seed}`.
  - plshare 매핑(안): `domain/{asset, importing, export, track, auth, media}`. ※ `import`는 코틀린 예약어 → `importing`.
  - 외부 통합 클라이언트(spotify/apple/storage)는 `infrastructure/`(또는 owning 도메인) 유지.
- **BaseTimeEntity** (`global/jpa`): `@MappedSuperclass` + `AuditingEntityListener` + `@CreatedDate/@LastModifiedDate`. **id는 UUID 유지**(polyenm의 Long IDENTITY는 차용 안 함). `@EnableJpaAuditing` 추가, `updated_at` 컬럼 Flyway 마이그레이션.

### Tier 3 — 크로스스택 (예정, FE 파급)
- **`ApiResponse<T>` envelope**(`global/response`): `{code, message, data}`로 성공 응답 통일. → FE api 클라이언트(`apiFetch`가 `.data` 언랩), 7개 api 모듈, fixtures, types, E2E 동시 수정 필요.
- **Controller + API 인터페이스 분리 + springdoc**: `XxxController : XxxApi`, Swagger 어노테이션은 인터페이스에. `springdoc-openapi` 의존성 추가.

## 가져오지 않는 것 (anti-adoption)
- BaseEntity의 **Long `@GeneratedValue` IDENTITY id** — plshare2는 의도적으로 UUID.
- 전 엔티티 일괄 **soft-delete** — 실제 필요한 도메인에만.
- `@CurrentUserId` 리졸버 — 실제 Spring Security 인증(현재는 grantId 쿠키) 도입 후에.
- `PageResponse` / `ServiceDate` — 페이지네이션/날짜 로직이 생기면 그때.

## 코드 스타일
- 서비스/엔티티에 **의도·불변식 KDoc**(왜 이렇게 하는지). polyenm 스타일.
- DTO 변환은 DTO의 `companion object from(...)`에 둔다(현행 유지).
- demo/prod는 `@Profile`로 분기, 실제 어댑터는 `@Profile("!demo")`.
