# Security policy

## Supported versions

Security reports are currently accepted for the latest source on the default branch. There are no released binaries yet.

## Reporting a vulnerability

Do not publish exploit details or pairing tokens in an issue. Use GitHub's private vulnerability reporting for the repository when enabled, or contact the maintainers privately through the repository's GitHub profile. Include affected commit, impact, reproduction steps, and any mitigations.

## Security model

GamePadX is designed for a trusted local network. The server binds to a private IPv4 address and requires an ephemeral random token, but the initial WebSocket connection is not encrypted. A hostile device on the same network may observe or interfere with traffic. Do not expose the port to the public internet or use untrusted/guest Wi-Fi.
