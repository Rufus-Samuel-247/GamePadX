# Contributing

Thanks for helping improve GamePadX. The project is early and hardware behavior needs validation on real devices.

## Before opening a change

- Keep network and input changes consistent with [protocol v1](docs/protocol.md).
- Do not add cloud services, telemetry, secrets, signing files, or router-facing defaults.
- Keep operating-system-specific virtual-controller code behind a replaceable boundary as the server grows.
- Update setup and security documentation when behavior or prerequisites change.

## Validation

Run the Android debug build and Windows server build from [Development](docs/development.md). For control/network changes, include device, Android version, Windows version, driver version, and a reproducible test description. Do not claim hardware behavior based only on compilation.

## Pull requests

Use a focused title, describe user-visible behavior, list tests actually run, and call out unverified hardware or driver dependencies. Avoid bundling generated build output.
