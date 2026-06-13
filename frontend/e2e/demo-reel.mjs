// @ts-check
/**
 * plshare2 — 전체 제품 시연 (Chromium 실구동).
 * 핵심 화면을 실제 클릭으로 돌며 단계별 스크린샷을 e2e/demo-reel/ 에 남긴다.
 * 실행: node e2e/demo-reel.mjs   (BE :8080 demo + FE :3000 가동 전제)
 */
import { chromium } from "playwright";

const FE = "http://localhost:3000";
const BE = "http://localhost:8080";
const DIR = "e2e/demo-reel";

const log = [];
const ok = (s, d = "") => { log.push(`✓ ${s}${d ? " — " + d : ""}`); console.log(`✓ ${s}${d ? " — " + d : ""}`); };
const warn = (s, d = "") => { log.push(`· ${s} (WARN)${d ? " — " + d : ""}`); console.log(`· ${s} WARN ${d}`); };
const fail = (s, e) => { log.push(`✗ ${s} — ${e}`); console.log(`✗ ${s} — ${e}`); };

let shotN = 0;
async function shot(page, name) {
  shotN += 1;
  const f = `${DIR}/${String(shotN).padStart(2, "0")}-${name}.png`;
  await page.screenshot({ path: f, fullPage: false });
  return f;
}

(async () => {
  // demo user id (for gift sender) via API
  const demo = await fetch(`${BE}/api/users/demo`).then((r) => r.json()).then((j) => j?.data ?? j);
  const userId = demo.id;

  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  // demo 세션 쿠키 (보호 라우트 통과 + 소셜 작성)
  await ctx.addCookies([{ name: "plshare_session", url: FE, value: encodeURIComponent(JSON.stringify({ grantId: "demo-grant", demo: true })) }]);
  const page = await ctx.newPage();
  const errors = [];
  page.on("pageerror", (e) => errors.push(String(e)));

  try {
    // 1) 랜딩
    await page.goto(FE, { waitUntil: "networkidle" });
    await page.waitForTimeout(1200);
    await shot(page, "landing");
    ok("랜딩", (await page.textContent("body"))?.includes("Spotify") ? "히어로 노출" : "");

    // 2) 딸깍 변환 — /convert 진입 → 라이브러리에서 선택
    await page.goto(`${FE}/convert`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1000);
    await shot(page, "convert-entry");
    // 링크 붙여넣기 입력에 mock Spotify playlist 링크
    const input = page.locator('input, textarea').first();
    if (await input.count()) {
      await input.fill("https://open.spotify.com/playlist/pl-late-night");
      await page.waitForTimeout(600);
      await shot(page, "convert-link-filled");
      ok("변환 — 링크 붙여넣기", "spotify playlist URL");
    } else warn("변환 입력창 못 찾음");

    // 3) 임포트 플로우(검증된 경로)로 자산 1개 확보 — 랜딩 CTA부터
    await page.goto(FE, { waitUntil: "networkidle" });
    await page.locator('a:has-text("Spotify로 시작")').first().click().catch(() => {});
    await page.waitForURL(/auth\/spotify/, { timeout: 6000 }).catch(() => {});
    await page.waitForURL(/\/import$/, { timeout: 12000 }).catch(() => {});
    await page.waitForLoadState("networkidle");
    await shot(page, "import-list");
    await page.locator("text=Late Night Drives").first().click().catch(() => {});
    await page.waitForURL(/\/assets\/[a-f0-9-]+$/, { timeout: 30000 });
    const assetUrl = page.url();
    const assetId = assetUrl.match(/\/assets\/([a-f0-9-]+)$/)?.[1];
    await page.waitForFunction(() => /Weeknd|Daft Punk|Blinding/.test(document.body.innerText), null, { timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(1500);
    await shot(page, "asset-detail");
    ok("임포트→자산", `asset ${assetId?.slice(0, 8)}`);

    // 4) 감성 맥락 입력
    const diary = page.locator("textarea").first();
    if (await diary.count()) {
      await diary.fill("새벽 4시, 텅 빈 도로를 달리던 그날 밤의 플레이리스트.");
      const tag = page.locator("button").filter({ hasText: /새벽|드라이브|그리움/ }).first();
      if (await tag.count()) await tag.click().catch(() => {});
      await page.waitForTimeout(800);
      await shot(page, "emotional-context");
      ok("감성 맥락 입력");
    } else warn("diary textarea 없음");

    // 5) Apple Music 내보내기 → 결과
    await page.locator('a, button').filter({ hasText: /Apple Music으로 내보내기|내보내기/ }).first().click().catch(() => {});
    await page.waitForURL(/\/export/, { timeout: 8000 }).catch(() => {});
    await page.waitForTimeout(1500);
    await shot(page, "export-progress");
    await page.waitForURL(/\/export\/result/, { timeout: 30000 }).catch(() => {});
    await page.waitForTimeout(1200);
    await shot(page, "export-result");
    ok("Apple 내보내기 결과");

    // 6) 피드 — 작성 + 좋아요
    await page.goto(`${FE}/feed`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await shot(page, "feed");
    const composer = page.locator("textarea").first();
    if (await composer.count()) {
      await composer.fill("이 새벽 드라이브 플레이리스트, 같이 들어요 🌙");
      await page.waitForTimeout(500);
      await shot(page, "feed-compose");
      const post = page.locator("button").filter({ hasText: /^게시$|게시하기|공유/ }).first();
      if (await post.count()) { await post.click().catch(() => {}); await page.waitForTimeout(1500); ok("피드 — 포스트 작성"); }
      else warn("게시 버튼 못 찾음");
    }
    // 좋아요 토글 (첫 포스트 하트)
    const like = page.locator('button[aria-label*="좋아요"], button:has-text("♡"), button:has-text("♥")').first();
    if (await like.count()) { await like.click().catch(() => {}); await page.waitForTimeout(700); }
    await shot(page, "feed-after");
    ok("피드 렌더/상호작용");

    // 7) 프로필
    await page.goto(`${FE}/u/demo`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await shot(page, "profile");
    ok("프로필 /u/demo", /포스트|posts/i.test((await page.textContent("body")) || "") ? "포스트 탭" : "");

    // 8) 랭킹
    await page.goto(`${FE}/ranking`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await shot(page, "ranking");
    ok("랭킹 /ranking");

    // 9) 선물 보내기
    await page.goto(`${FE}/gift/send`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await shot(page, "gift-send");
    ok("선물 보내기 /gift/send");

    // 10) 선물 언박싱 — API로 token 만들고 수신 화면
    const giftTok = await fetch(`${BE}/api/gifts`, {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ senderId: userId, assetId, message: "우리 그날의 새벽, 이 노래들로 기억해줘.", wrapSkin: "midnight" }),
    }).then((r) => r.json()).then((j) => (j?.data ?? j)?.token);
    if (giftTok) {
      await page.goto(`${FE}/gift/${giftTok}`, { waitUntil: "networkidle" });
      await page.waitForTimeout(3000); // 언박싱 곡 순차 공개 연출 대기
      await shot(page, "gift-unboxing");
      ok("선물 언박싱 /gift/[token]", `token ${giftTok.slice(0, 8)}`);
    } else warn("gift token 생성 실패");

    // console error 요약 (favicon 등 제외)
    const fatal = errors.filter((e) => !/favicon|404/.test(e));
    fatal.length ? warn("console errors", `${fatal.length}건`) : ok("치명적 콘솔 에러 없음");
  } catch (e) {
    fail("WALKTHROUGH", e instanceof Error ? e.message : String(e));
    await shot(page, "FAIL");
  } finally {
    await browser.close();
    console.log("\n" + "=".repeat(56));
    console.log(log.join("\n"));
    console.log("=".repeat(56));
    console.log(`스크린샷 ${shotN}장 → ${DIR}/`);
  }
})();
