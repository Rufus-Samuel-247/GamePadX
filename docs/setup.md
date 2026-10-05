# Setup

## Android app

1. Install Android Studio, JDK 17, and Android SDK Platform 35.
2. Open the repository root in Android Studio and let Gradle sync.
3. Select the `app` configuration and a connected Android device or emulator.
4. Build with `gradle :mobile:app:assembleDebug` when Gradle 8.9+ is available.

The app requests network access only. Landscape is enforced for the controller screen, and the display stays awake while the activity is open.

## Windows companion

1. Install the .NET 8 SDK.
2. Install and verify the ViGEmBus driver separately. GamePadX does not ship a driver installer.
3. Connect the PC to a trusted private Wi-Fi network.
4. From the repository root, run:

   ```powershell
   dotnet run --project server\GamePadX.Server\GamePadX.Server.csproj
   ```

5. If Windows Firewall prompts, allow the app on the **Private** network profile only. Never create a router port-forward for GamePadX.
6. Copy the printed private IP and temporary token into the phone's **PAIR PC** dialog.

The default listening port is `8765`. Set `GAMEPADX_PORT` in the process environment to change it. Stop the server with Ctrl+C; restart it to rotate the pairing token.

## Same-network checks

- Both devices must be on the same Wi-Fi subnet; guest Wi-Fi/client isolation may block peer traffic.
- The PC must have an RFC1918 IPv4 address. IPv6-only and public-network setups are not supported.
- If pairing times out, confirm the address shown by the server, private firewall rule, port, and that no VPN is routing either device away from the LAN.
