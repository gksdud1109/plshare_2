# Implementation Contract And Open Issues

제품 범위는 `docs/20-product/strategy/product-baseline-v2.md`를 따른다.
모든 API 응답은 `{ code, message, data }` envelope를 사용한다.

## Current API Contract

인증 요청:

```http
Authorization: Bearer <application-session-token>
```

### Session

```http
GET /api/auth/session
```

```json
{
  "userId": "uuid",
  "spotifyGrantId": "uuid-or-null"
}
```

### Import

```http
POST /api/imports
X-Idempotency-Key: <client-generated-key>
Content-Type: application/json
```

```json
{
  "playlistId": "provider-playlist-id",
  "sourcePlatform": "spotify | youtube"
}
```

```http
GET /api/imports/{jobId}
```

Import status values: `queued`, `running`, `completed`, `failed`.

### Assets

```http
GET /api/assets
GET /api/assets/{assetId}
PATCH /api/assets/{assetId}
POST /api/assets/{assetId}/share
```

인증된 사용자는 자기 `ownerId`와 일치하는 asset만 조회·수정·공유할 수 있다.
공개 공유 링크만 `GET /api/share/{token}`으로 익명 접근한다.

### Export

```http
POST /api/exports
X-Idempotency-Key: <client-generated-key>
Content-Type: application/json
```

```json
{
  "assetId": "uuid",
  "targetPlatform": "youtube | apple"
}
```

```http
GET /api/exports/{jobId}
GET /api/exports/{jobId}/result
```

Export status values:
`queued`, `matching`, `ready`, `executing`, `completed`, `partial`, `failed`.

## Required Engineering Rules

- 외부 provider 호출은 장시간 DB transaction 안에서 실행하지 않는다.
- mutation 요청은 `X-Idempotency-Key`를 사용한다.
- idempotency key와 import/export job은 사용자 소유권 경계를 넘지 않는다.
- provider timeout, 429, token expiry는 사용자에게 실패 상태로 노출한다.
- 프로덕션 장애를 fixture 성공 데이터로 대체하지 않는다.

## Open Issues

1. **Production storage is not implemented.**
   `S3StorageAdapter`는 현재 fail-fast stub이다. Emotional Context 사진 업로드를
   프로덕션에서 열기 전에 AWS SDK v2 presigner와 authenticated delete를 구현해야 한다.
2. **Live OAuth has not been integration-tested.**
   Spotify PKCE, Google incremental YouTube consent, refresh token 보존, 동일 사용자
   grant 연결을 실제 provider 계정으로 검증해야 한다.
3. **Production migrations need a PostgreSQL smoke test.**
   V9-V11의 ownership/OAuth FK와 기존 nullable legacy row 호환을 staging DB에서 검증한다.
4. **YouTube quota operations need policy.**
   search 100 units + playlist write 비용에 대한 일일 예산, 사용자별 제한,
   quota exhaustion UX를 운영 기준으로 확정해야 한다.
5. **Application sessions have no server-side revocation.**
   현재 HMAC 세션은 만료 전 강제 폐기할 수 없다. 계정 탈취 대응이 필요해지면
   session version 또는 revocation store를 추가한다.
