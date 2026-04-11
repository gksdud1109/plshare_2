# [Technical Decision] 기술 스택 확정 및 아키텍처 구조 v0.1

## 1. 확정 기술 스택 (The Stack)

| 구분 | 기술 | 선정 사유 |
| :--- | :--- | :--- |
| **Frontend** | **Next.js (App Router)** | SEO(공유 페이지), 빠른 UI 개발, BFF(Backend for Frontend) 역할 수행 가능. |
| **Backend** | **Kotlin / Spring Boot** | **핵심 로직(정규화, 어댑터)**의 타입 안정성, 비동기 작업(`@Async`, `WebClient`) 처리 우수성. |
| **Database** | **Supabase (PostgreSQL)** | Managed DB로서의 운영 편의성, 향후 실시간 기능 확장성. |
| **Auth** | **Supabase Auth + Spring Security** | 소셜 로그인(FE)과 API 권한 제어(BE)의 조화. |
| **Storage** | **Supabase Storage** | 커버 이미지 등 자산 파일 저장. |
| **Infrastructure** | **Vercel (FE), Cloud Run/AWS (BE)** | 배포 및 확장성 고려. |

---

## 2. 역할 분담 (Responsibility Mapping)

오케스트레이터가 경고한 '애매한 혼합'을 방지하기 위해 책임을 명확히 자릅니다.

### A. Next.js (Frontend / Public Page)
- **UI/UX:** Luxury/Refined 디자인 시스템 구현.
- **Public Share Page:** 비로그인 사용자를 위한 고성능 서버 사이드 렌더링(SSR).
- **Client Auth:** Supabase Auth를 통한 소셜 로그인 세션 관리.
- **Proxy:** 일부 백엔드 API 호출을 위한 BFF 역할.

### B. Kotlin/Spring Boot (Core Ledger / Engine)
- **Normalization Engine:** ISRC 기반 트랙 식별 및 매칭 로직 (이 제품의 핵심 자산).
- **Platform Adapters:** Spotify API(Read), Apple Music API(Write) 연동 및 토큰 관리.
- **Background Jobs:** 플레이리스트 가져오기/내보내기 비동기 처리 및 재시도 로직.
- **Data Ledger:** 자산(PA)의 무결성을 보장하는 원장 관리.

### C. Supabase (Data / Infrastructure)
- **PostgreSQL:** 모든 정규화된 데이터 및 사용자 자산 저장.
- **Auth:** 사용자 계정 정보 및 세션 제공.
- **Storage:** 사용자가 업로드한 커버 이미지 저장.

---

## 3. 이 아키텍처를 선택한 전략적 이유

1. **도메인 복잡도 대응:** 단순 CRUD가 아니라 외부 API 연동과 데이터 정규화가 핵심인 제품입니다. Spring Boot는 이러한 '파이프라인'형 로직을 구조화하는 데 매우 강력합니다.
2. **비동기 안정성:** 100곡 이상의 플레이리스트를 내보내는 작업은 시간이 걸리는 작업입니다. Spring의 비동기 큐 처리와 에러 핸들링은 MVP의 품질을 한 단계 높여줍니다.
3. **확장성:** 향후 양방향 어댑터 확장이나 P1 proof signal 같은 후속 기능이 추가될 때도 백엔드가 이미 Kotlin/Spring으로 분리되어 있으면 기술적 부채 없이 확장이 가능합니다.

---

## 4. 초기 구축 전략 (Implementation Strategy)

- **Phase 1:** Supabase Auth와 Next.js를 먼저 연결하여 로그인/프로필 환경 구축.
- **Phase 2:** Spring Boot에서 Spotify OAuth 연동 및 ISRC 추출 로직 우선 구현.
- **Phase 3:** Spring Boot의 비동기 작업을 통해 Apple Music 플레이리스트 생성 기능 연동.
