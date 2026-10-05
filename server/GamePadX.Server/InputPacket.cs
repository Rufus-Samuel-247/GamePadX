namespace GamePadX.Server;

public sealed record InputPacket
{
    public string Type { get; init; } = string.Empty;
    public long Timestamp { get; init; }
    public string[] Buttons { get; init; } = [];
    public StickState LeftStick { get; init; } = new();
    public StickState RightStick { get; init; } = new();
    public TriggerState Triggers { get; init; } = new();
}

public sealed record StickState
{
    public float X { get; init; }
    public float Y { get; init; }
}

public sealed record TriggerState
{
    public float L2 { get; init; }
    public float R2 { get; init; }
}
