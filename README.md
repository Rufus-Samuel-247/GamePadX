# GamePadX

Turn your Android phone into a wireless game controller for your PC. GamePadX sends input directly over your local Wi-Fi network; it does not use a cloud service.

> **Status: early MVP.** The Android controller, manual token pairing, low-rate WebSocket input transport, latency display, and Windows virtual-controller adapter are implemented. Builds and real phone-to-PC play have not yet been verified in this environment. See [Development status](#development-status) before testing.

## What works in this MVP

- Landscape Android controller with multi-touch D-pad, A/B/X/Y, Start/Select, L1/R1, pressure-style L2/R2, and two analog sticks.
- Adjustable stick sensitivity and screen-awake gameplay.
- Manual pairing with a random 128-bit token; local private IPv4 address validation on the phone and private-interface binding on the PC.
- WebSocket state packets, ping measurement, connection-loss indication, and automatic reconnect.
- Experimental Windows Xbox 360-compatible virtual input through the archived ViGEm.NET client and ViGEmBus driver.

QR scanning, saved/editable profiles, haptics, a dedicated input testing view, and a setup wizard are not implemented yet. They are not presented as working controls.

## Requirements

- Android Studio with JDK 17 and Android SDK Platform 35.
- Windows 10/11 PC with the .NET 8 SDK.
- Windows virtual-controller support currently depends on ViGEmBus, whose upstream project is retired. GamePadX does not bundle or install drivers; treat this backend as experimental.
- Phone and PC connected to the same trusted private Wi-Fi network. Allow the selected port (default `8765`) on the Windows **Private** firewall profile only.

## Build and run

Clone the repository:

```powershell
git clone https://github.com/Rufus-Samuel-247/GamePadX.git
cd GamePadX
```

### Android

Open the repository root in Android Studio and allow Gradle sync. With Gradle 8.9+ installed and JDK 17 selected, build the debug APK:

```powershell
gradle :mobile:app:assembleDebug
```

The APK is written to `mobile/app/build/outputs/apk/debug/app-debug.apk`. A Gradle wrapper is not included yet; see [Development status](#development-status).

### Windows PC server

Install the .NET 8 SDK and ViGEmBus first, then run:

```powershell
dotnet run --project server\GamePadX.Server\GamePadX.Server.csproj
```

The server prints its private IPv4 address and a new pairing token each run. In the Android app, choose **PAIR PC**, enter that address and token, then connect. The token is temporary and must be kept private. The server listens on port `8765` by default; set `$env:GAMEPADX_PORT = "8765"` before launching to select a different port.

## Security notes

- Input stays on the local network; there is no account, relay, telemetry, or cloud transport.
- The server binds only to a detected RFC1918 IPv4 address and requires a random token for every WebSocket connection.
- Use a trusted Wi-Fi network and a Private-only firewall rule. Do not forward the server port from your router or expose it to the public internet.
- The initial WebSocket transport is unencrypted. The random token is a local-network pairing control, not protection against a hostile device already on the same network.
- Never commit pairing tokens, signing material, or personal network details.

## Screenshots

No device screenshots are available yet. Add tested Android captures under `docs/screenshots/` after verifying the UI on real devices.

## Troubleshooting

- **The phone cannot connect:** confirm both devices are on the same private Wi-Fi, use the IPv4 address printed by the server, and allow the selected port on the Windows Private firewall profile.
- **The server exits during startup:** verify the .NET 8 SDK is installed, the PC has a private IPv4 address, and ViGEmBus is installed and operational.
- **Pairing is rejected:** restart the server and enter its newly printed token; tokens rotate on every launch.
- **The server connects but games see no controller:** the ViGEm backend is experimental and depends on the retired ViGEmBus stack. Confirm the virtual device appears in Windows game-controller settings before testing a game.
- **Inputs stop after the phone sleeps or Wi-Fi changes:** return to the app and reconnect; automatic reconnect is attempted while the activity remains alive.

## FAQ

**Does GamePadX use a cloud server?** No. The phone communicates directly with the PC over the local network.

**Can I use it on Linux or macOS?** Not yet. The current virtual-controller output is Windows-specific.

**Are QR pairing and custom profiles available?** Not yet. Pairing is manual, and profile editing/persistence are planned work.

## Development status

This is a starting MVP, not a release-ready application. Android and server compilation, the ViGEm API integration, connection-loss recovery, and real button/stick/trigger behavior still need to be verified on supported devices. This workspace currently has only Java 8 and .NET runtimes, with no Android SDK, Gradle, or .NET SDK installed, so those build and hardware checks cannot be claimed as passing.

Next milestones:

1. Add a Gradle wrapper and automated protocol/server tests.
2. Build and install on a physical Android device; test simultaneous touches, release behavior, latency, and reconnect.
3. Select a maintained Windows virtual-controller backend before claiming production readiness; ViGEm upstream is retired.
4. Add QR pairing, persisted profiles/layout editing, haptics, and a controller test view.
5. Add Linux/macOS virtual-controller backends after the Windows path is stable.

## Documentation

- [Architecture](docs/architecture.md)
- [Protocol](docs/protocol.md)
- [Setup](docs/setup.md)
- [Development](docs/development.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## License

MIT. See [LICENSE](LICENSE).
