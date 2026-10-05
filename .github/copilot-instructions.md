# GamePadX workspace instructions

- Keep input transport local-network-only by default; do not add cloud relays, telemetry, credentials, or public bind defaults.
- Keep Android and server packet fields aligned with `docs/protocol.md` and `protocol/input-v1.json`.
- Do not describe QR pairing, saved profiles, layout editing, haptics, or the test screen as implemented until those paths work end to end.
- Treat the ViGEm backend as experimental because upstream is retired; do not claim release readiness without a maintained and tested Windows backend.
- Run the Android and server builds when their SDKs are available. Report device/driver checks separately from compile results.
