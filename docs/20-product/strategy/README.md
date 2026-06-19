# Strategy

이 폴더에는 제품 방향과 범위를 고정하는 전략 문서를 둔다.

현재 문서:
- `product-baseline-v2.1.md` (`CURRENT`)
- `product-baseline-v2.md` (superseded)
- `platform-strategy-v2-research-v0.1.md` (historical research)
- `prd-v0.2.md` (legacy Apple-first PRD)

현재 고정 기준:
- `product-baseline-v2.1.md`가 범위의 단일 기준이다
- P0는 Google identity, YouTube 기반 취향 자산 생성, Emotional Context,
  gift/unboxing, social, share surfaces다
- Spotify·Apple 계정 연동과 교차 플랫폼 변환은 feature flag OFF인 dormant 범위다
- YouTube 계정으로 내보내기는 P1이다
- Direct messages, B2B 데이터 패키징, 온체인 민팅은 MVP 범위 밖이다

운영 규칙:
- 최신 PRD는 이 폴더 루트에 둔다
- 이전 PRD 버전은 `archive/`에 둔다
- 요구사항 문서와 리서치 문서는 각각 별도 폴더에서 관리한다
- 일시적 정렬 메모는 반영 후 `archive/`로 내린다

권장 읽기 순서:
1. `product-baseline-v2.1.md`
2. `platform-strategy-v2-research-v0.1.md` (historical context only)
3. `product-baseline-v2.md`와 `prd-v0.2.md` (superseded/legacy reference only)
