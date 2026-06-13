export function demoFixturesEnabled(): boolean {
  return process.env.NEXT_PUBLIC_DEMO_MODE === "true";
}
