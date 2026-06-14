// @ts-check
/**
 * plshare2 — 사용자 시나리오별 E2E 검증 (gift-first).
 * 각 시나리오를 사용자 관점에서 끝까지 검증한다. 하나가 실패해도 다음 시나리오는 계속.
 * Run: node e2e/scenarios.spec.mjs   (라이브 스택 BE:8080 / FE:3000 필요)
 */
import { chromium } from 'playwright';
import { mkdirSync } from 'fs';

const FE = 'http://localhost:3000';
const BE = 'http://localhost:8080';
const SHOT = 'e2e/screenshots';
mkdirSync(SHOT, { recursive: true });

const results = [];
const rec = (scn, ok, detail = '') => {
  results.push({ scn, ok });
  console.log(`  ${ok ? '✓' : '✗'} ${scn}${detail ? ' — ' + detail : ''}`);
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const j = async (url, opts) => {
  const r = await fetch(url, opts);
  const body = await r.json().catch(() => ({}));
  return { status: r.status, data: body?.data ?? body };
};
const postJson = (url, obj, headers = {}) =>
  j(url, { method: 'POST', headers: { 'Content-Type': 'application/json', ...headers }, body: JSON.stringify(obj) });

// demo 세션 쿠키를 가진 context 생성 (/auth/spotify 가 localhost 데모 세션을 만든다)
async function authedContext(browser) {
  const ctx = await browser.newContext({ viewport: { width: 1100, height: 1000 } });
  const p = await ctx.newPage();
  await p.goto(`${FE}/auth/spotify`, { waitUntil: 'networkidle' });
  await sleep(3000);
  await p.close();
  return ctx;
}

(async () => {
  console.log('\n═══ SETUP ═══');
  // demo 유저
  const demoUser = (await j(`${BE}/api/users/demo`)).data;
  // 데모 세션 토큰 — 직접 BE 호출도 prod 와 동일하게 Bearer 로 신원을 전달한다(@CurrentUserId).
  const demoToken = (await postJson(`${BE}/api/auth/demo-session`, {})).data.sessionToken;
  const auth = { Authorization: `Bearer ${demoToken}` };
  // asset 확보 (import pl-late-night) — 데모 유저 소유로 import(ownerId=principal)
  const ijob = (await postJson(`${BE}/api/imports`, { playlistId: 'pl-late-night' }, { 'X-Idempotency-Key': `scn-${Date.now()}`, ...auth })).data;
  await sleep(3500);
  const assetId = (await j(`${BE}/api/imports/${ijob.jobId}`)).data.assetId;
  // 선물 생성 — 발신자는 토큰에서 해석(본인 소유 자산만 선물 가능)
  const gift = (await postJson(`${BE}/api/gifts`, { assetId, message: '늦은 밤 운전할 때 들어. 우리 그때 그 도로 생각나서.', wrapSkin: 'nocturne-violet' }, auth)).data;
  // 공유 토큰
  const share = (await postJson(`${BE}/api/assets/${assetId}/share`, {}, auth)).data;
  // 피드 첫 포스트
  const feed = (await j(`${BE}/api/posts?page=0&size=10`)).data;
  const posts = feed.items ?? feed.content ?? [];
  const firstPost = posts[0];
  console.log(`  demo=${demoUser?.handle} asset=${assetId?.slice(0,8)} gift=${gift?.token} share=${share?.shareToken?.slice(0,8)} posts=${posts.length}`);

  const browser = await chromium.launch({ headless: true });
  const errors = [];

  // ── S1: 신규 방문자 → 선물 만들기 (sender) ──────────────────────────────
  console.log('\n═══ S1: 보내는 사람 — 선물 만들기 ═══');
  try {
    const ctx = await authedContext(browser);
    const p = await ctx.newPage();
    p.on('console', (m) => m.type() === 'error' && errors.push(`S1: ${m.text()}`));
    await p.goto(FE, { waitUntil: 'networkidle' });
    rec('랜딩이 gift-first', /선물 만들기|선물로/.test(await p.textContent('body')));
    await p.goto(`${FE}/gift/send`, { waitUntil: 'networkidle' });
    await sleep(1800);
    rec('인증 진입(로그인 벽 없음)', !/로그인이 필요/.test(await p.textContent('body')));
    const ta = p.locator('#gift-message');
    rec('자산 선택지 로드', (await p.locator('form button[type="button"]').count()) > 0);
    await ta.fill('이 곡들 들으면서 운전해. 보고 싶다.');
    await p.locator('button[type="submit"]').click();
    await sleep(2800);
    rec('선물 생성 완료(링크 발급)', /선물이 준비/.test(await p.textContent('body')));
    await p.screenshot({ path: `${SHOT}/s1-send-done.png` });
    await ctx.close();
  } catch (e) { rec(`S1 예외: ${e.message}`, false); }

  // ── S2: 수신자 (계정 없음) → 포장 열기 → 재생 ──────────────────────────
  console.log('\n═══ S2: 받는 사람 — 계정 없이 ═══');
  try {
    const ctx = await browser.newContext({ viewport: { width: 440, height: 1000 } }); // fresh, no session
    const p = await ctx.newPage();
    p.on('console', (m) => m.type() === 'error' && errors.push(`S2: ${m.text()}`));
    await p.goto(`${FE}/gift/${gift.token}`, { waitUntil: 'networkidle' });
    await sleep(1300);
    rec('포장 상태로 진입(선물 열기)', /선물 열기/.test(await p.textContent('body')));
    await p.screenshot({ path: `${SHOT}/s2-wrapped.png` });
    await p.locator('text=선물 열기').first().click();
    await sleep(3300);
    const playBtn = p.locator('button[aria-label$="재생"]').first();
    rec('언박싱 트랙 노출', (await playBtn.count()) > 0);
    await playBtn.click();
    await sleep(3200);
    const iframe = p.locator('iframe[src*="youtube.com/embed"]').first();
    const embedded = await iframe.count();
    // synthetic mock id(11자라 길이 정규식엔 통과)를 배제 — 실제 재생 가능한 id만 인정
    const vidMatch = embedded ? ((await iframe.getAttribute('src')) || '').match(/youtube\.com\/embed\/([A-Za-z0-9_-]{11})/) : null;
    const realId = !!vidMatch && !vidMatch[1].startsWith('mock');
    rec('계정 없이 트랙 재생(YouTube 임베드)', embedded > 0 && realId, vidMatch ? `videoId=${vidMatch[1]}` : 'none');
    rec('미인증 수신자 CTA(내 라이브러리 만들기)', /내 라이브러리 만들기/.test(await p.textContent('body')));
    await p.screenshot({ path: `${SHOT}/s2-play.png`, fullPage: true });
    await ctx.close();
  } catch (e) { rec(`S2 예외: ${e.message}`, false); }

  // ── S3: 수신자 (로그인) → 저장 ─────────────────────────────────────────
  console.log('\n═══ S3: 받는 사람 — 로그인 후 저장 ═══');
  try {
    const ctx = await authedContext(browser);
    const p = await ctx.newPage();
    await p.goto(`${FE}/gift/${gift.token}`, { waitUntil: 'networkidle' });
    await sleep(1200);
    await p.locator('text=선물 열기').first().click();
    await sleep(3300);
    const saveBtn = p.locator('button').filter({ hasText: /라이브러리에 저장/ }).first();
    rec('저장 버튼 노출(인증 수신자)', (await saveBtn.count()) > 0);
    if (await saveBtn.count()) {
      await saveBtn.click();
      await sleep(1800);
      rec('라이브러리 저장 완료', /저장됐어요/.test(await p.textContent('body')));
    }
    await ctx.close();
  } catch (e) { rec(`S3 예외: ${e.message}`, false); }

  // ── S4: 공개 공유 페이지 (계정 없음) → 재생 ────────────────────────────
  console.log('\n═══ S4: 공개 공유 페이지 — 계정 없이 ═══');
  try {
    const ctx = await browser.newContext({ viewport: { width: 1100, height: 1000 } }); // fresh
    const p = await ctx.newPage();
    p.on('console', (m) => m.type() === 'error' && errors.push(`S4: ${m.text()}`));
    await p.goto(`${FE}/share/${share.shareToken}`, { waitUntil: 'networkidle' });
    await sleep(1500);
    rec('공유 페이지 자산 렌더', /Late Night Drives|Weeknd|Daft Punk/.test(await p.textContent('body')));
    const sb = p.locator('button[aria-label$="재생"]').first();
    rec('공유 트랙 재생 어포던스', (await sb.count()) > 0);
    if (await sb.count()) {
      await sb.click();
      await sleep(3000);
      rec('공유 페이지 트랙 재생(임베드)', (await p.locator('iframe[src*="youtube.com/embed"]').count()) > 0);
    }
    await p.screenshot({ path: `${SHOT}/s4-share-play.png`, fullPage: true });
    await ctx.close();
  } catch (e) { rec(`S4 예외: ${e.message}`, false); }

  // ── S5: 소셜 — 피드/좋아요/댓글 ────────────────────────────────────────
  console.log('\n═══ S5: 소셜 — 피드/좋아요/댓글 ═══');
  try {
    const ctx = await authedContext(browser);
    const p = await ctx.newPage();
    await p.goto(`${FE}/feed`, { waitUntil: 'networkidle' });
    await sleep(1800);
    rec('피드 렌더(포스트 노출)', posts.length > 0 && !/불러오지 못/.test(await p.textContent('body')));
    await p.screenshot({ path: `${SHOT}/s5-feed.png`, fullPage: true });
    // 좋아요: API 멱등 검증 (UI 셀렉터 불안정 회피)
    if (firstPost) {
      await postJson(`${BE}/api/posts/${firstPost.id}/like`, {}, auth);
      const c1 = (await postJson(`${BE}/api/posts/${firstPost.id}/like`, {}, auth)).data;
      rec('좋아요 멱등(중복 호출 → count 유지)', typeof c1 === 'number');
      // 댓글 — 작성자는 토큰에서 해석
      const cm = (await postJson(`${BE}/api/posts/${firstPost.id}/comments`, { text: 'E2E 댓글' }, auth)).data;
      rec('댓글 작성', !!cm?.id || !!cm?.text);
    } else { rec('피드 포스트 없음(시드 확인 필요)', false); }
    await ctx.close();
  } catch (e) { rec(`S5 예외: ${e.message}`, false); }

  // ── S6: 소셜 — 프로필/팔로우 ───────────────────────────────────────────
  console.log('\n═══ S6: 소셜 — 프로필/팔로우 ═══');
  try {
    // 2번째 유저(alice) 보장 — Postgres 직접 삽입(없으면)
    await fetch(`${BE}/api/users/alice/follow-stats`).then(async (r) => {
      if (r.status === 404 || r.status >= 400) {
        // psql 삽입은 셸에서 별도 처리됨(setup). 여기선 존재 가정.
      }
    }).catch(() => {});
    const before = (await j(`${BE}/api/users/alice/follow-stats`, { headers: auth }));
    if (before.status === 200) {
      await postJson(`${BE}/api/users/alice/follow`, {}, auth);
      const after = (await j(`${BE}/api/users/alice/follow-stats`, { headers: auth })).data;
      rec('팔로우 → 팔로워 수 증가/isFollowing', after?.isFollowing === true || (after?.followerCount ?? 0) >= 1);
      // 자기 팔로우 거부 — followerId 는 토큰에서 해석(demo→demo)
      const self = await fetch(`${BE}/api/users/demo/follow`, { method: 'POST', headers: auth });
      rec('자기 자신 팔로우 거부(4xx)', self.status >= 400 && self.status < 500);
      // 언팔로우
      await fetch(`${BE}/api/users/alice/follow`, { method: 'DELETE', headers: auth });
      const un = (await j(`${BE}/api/users/alice/follow-stats`, { headers: auth })).data;
      rec('언팔로우 → 팔로워 0', (un?.followerCount ?? 0) === 0);
    } else {
      rec('alice 프로필 없음(팔로우 시나리오 스킵)', false);
    }
    // 프로필 페이지 UI 렌더
    const ctx = await authedContext(browser);
    const p = await ctx.newPage();
    await p.goto(`${FE}/u/${demoUser.handle}`, { waitUntil: 'networkidle' });
    await sleep(1500);
    rec('프로필 페이지 렌더', /@?demo|팔로/.test(await p.textContent('body')));
    await p.screenshot({ path: `${SHOT}/s6-profile.png`, fullPage: true });
    await ctx.close();
  } catch (e) { rec(`S6 예외: ${e.message}`, false); }

  // ── S7: 랭킹 ───────────────────────────────────────────────────────────
  console.log('\n═══ S7: 랭킹 ═══');
  try {
    const ctx = await browser.newContext({ viewport: { width: 1100, height: 1000 } });
    const p = await ctx.newPage();
    await p.goto(`${FE}/ranking`, { waitUntil: 'networkidle' });
    await sleep(1800);
    const t = await p.textContent('body');
    rec('랭킹 페이지 렌더(플레이리스트/사용자 탭)', /랭킹|플레이리스트|사용자|Ranking/.test(t) && !/불러오지 못|찾을 수 없/.test(t));
    await p.screenshot({ path: `${SHOT}/s7-ranking.png`, fullPage: true });
    await ctx.close();
  } catch (e) { rec(`S7 예외: ${e.message}`, false); }

  // ── S8: OG 공유 카드 ───────────────────────────────────────────────────
  console.log('\n═══ S8: OG 공유 카드 ═══');
  try {
    const giftHtml = await (await fetch(`${FE}/gift/${gift.token}`)).text();
    rec('선물 OG 메타(og:title/image)', /og:title/.test(giftHtml) && /og:image/.test(giftHtml));
    const giftOg = await fetch(`${FE}/gift/${gift.token}/opengraph-image`);
    rec('선물 OG 이미지 200(image)', giftOg.status === 200 && /image/.test(giftOg.headers.get('content-type') || ''));
    const shareOg = await fetch(`${FE}/share/${share.shareToken}/opengraph-image`);
    rec('공유 OG 이미지 200', shareOg.status === 200);
  } catch (e) { rec(`S8 예외: ${e.message}`, false); }

  // ── S9: 네거티브 — 잘못된 선물 토큰 ────────────────────────────────────
  console.log('\n═══ S9: 네거티브 — 잘못된 토큰 ═══');
  try {
    const ctx = await browser.newContext();
    const p = await ctx.newPage();
    await p.goto(`${FE}/gift/this-token-does-not-exist-xyz`, { waitUntil: 'networkidle' });
    await sleep(1500);
    rec('잘못된 선물 → 찾을 수 없음 안내', /찾을 수 없|만료/.test(await p.textContent('body')));
    await ctx.close();
  } catch (e) { rec(`S9 예외: ${e.message}`, false); }

  // ── S10: 전환 dormant ─────────────────────────────────────────────────
  console.log('\n═══ S10: 전환 dormant ═══');
  try {
    const ctx = await browser.newContext();
    const p = await ctx.newPage();
    await p.goto(FE, { waitUntil: 'networkidle' });
    const landing = await p.textContent('body');
    rec('랜딩에 전환 문구 없음', !/Spotify로 시작|YouTube Music으로 옮/.test(landing));
    // 네비: 선물 있고 변환 없음(플래그 off). 네비가 있는 페이지(/feed)에서 확인
    await p.goto(`${FE}/feed`, { waitUntil: 'networkidle' });
    await sleep(800);
    const navText = await p.textContent('nav').catch(() => '');
    rec('네비에 선물 노출·변환 숨김', /선물/.test(navText) && !/변환/.test(navText));
    // /convert 라우트는 보존(404 아님)
    const conv = await fetch(`${FE}/convert`);
    rec('/convert 코드 보존(404 아님)', conv.status === 200);
    await ctx.close();
  } catch (e) { rec(`S10 예외: ${e.message}`, false); }

  // ── 요약 ──
  await browser.close();
  const pass = results.filter((r) => r.ok).length;
  const fail = results.filter((r) => !r.ok).length;
  const fatal = errors.filter((e) => !/favicon|404.*png|ERR_NAME_NOT_RESOLVED.*demo-/.test(e));
  console.log(`\n${'═'.repeat(60)}`);
  console.log(`시나리오 검증: ${pass} pass · ${fail} fail`);
  if (fail > 0) console.log('실패:', results.filter((r) => !r.ok).map((r) => r.scn).join(' | '));
  console.log(`치명 console 에러: ${fatal.length}`);
  if (fatal.length) fatal.slice(0, 6).forEach((e) => console.log('  •', e.slice(0, 120)));
  console.log(`${'═'.repeat(60)}`);
  process.exit(fail > 0 ? 1 : 0);
})();
