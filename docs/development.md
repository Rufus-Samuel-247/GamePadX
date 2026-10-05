# Development

## Repository layout

- `mobile/app`: Android Kotlin/Compose application.
- `server/GamePadX.Server`: ASP.NET Core WebSocket server and Windows ViGEm adapter.
- `protocol`: versioned wire protocol documentation and examples.
- `docs`: architecture, setup, and development notes.

## Local checks

Android debug build:

```powershell
gradle :mobile:app:assembleDebug
```

Windows server build/run:

```powershell
dotnet build server\GamePadX.Server\GamePadX.Server.csproj
dotnet run --project server\GamePadX.Server\GamePadX.Server.csproj
```

Builds require JDK 17, Android SDK 35, Gradle 8.9+, .NET 8 SDK, and the ViGEm client package restore. A Gradle wrapper and automated tests are planned but not present yet.

## Current gaps

- No QR pairing, saved profiles, layout editor, or profile persistence.
- No haptic feedback or dedicated controller test screen.
- No protocol version negotiation, TLS, or rate limiting.
- No automated server/client integration tests; physical device and Windows driver testing remain necessary.
- Windows adapter is currently coupled directly into server startup; other OS backends are not implemented.
