# Input protocol v1

Transport: WebSocket over the local network. The server endpoint is `/ws?token=<32 lowercase hex characters>`. Messages are UTF-8 JSON text frames, limited to 4 KiB. The client sends complete state snapshots, not deltas, so lost updates are corrected by the next packet.

## Input

```json
{
  "type": "input",
  "timestamp": 123456789,
  "buttons": ["A", "L1"],
  "leftStick": { "x": 0.45, "y": -0.72 },
  "rightStick": { "x": 0.0, "y": 0.0 },
  "triggers": { "L2": 0.0, "R2": 0.85 }
}
```

- `timestamp` is Android elapsed realtime in milliseconds; it is used for ordering/diagnostics, not wall-clock time.
- `buttons` lists currently held controls. Supported names: `A`, `B`, `X`, `Y`, `L1`, `R1`, `Start`, `Select`, `Home`, `Up`, `Down`, `Left`, `Right`, `L3`, `R3`.
- Stick `x` and `y` are clamped to `[-1, 1]`; positive `y` means up.
- Trigger values are clamped to `[0, 1]`.
- A missing button means released. Every packet updates all buttons and axes.

## Latency probe

Client request:

```json
{ "type": "ping", "timestamp": 123456789 }
```

Server response:

```json
{ "type": "pong", "timestamp": 123456789 }
```

The client computes round-trip milliseconds from its monotonic clock. This is not a one-way network latency measurement.

## Pairing and failure behavior

- The server generates a fresh random 128-bit token at startup. Clients without a matching token receive HTTP 401 before WebSocket upgrade.
- The server rejects non-text frames, malformed packets, and messages over 4 KiB. An input packet may contain at most 16 button names.
- This MVP does not negotiate protocol versions, encrypt frames, or implement rate-limit enforcement. Use a trusted private network and do not expose the port publicly.
