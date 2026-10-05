using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;

namespace GamePadX.Server;

public sealed class ViGEmVirtualController : IDisposable
{
    private static readonly (string Name, Xbox360Button Button)[] Buttons =
    [
        ("A", Xbox360Button.A), ("B", Xbox360Button.B),
        ("X", Xbox360Button.X), ("Y", Xbox360Button.Y),
        ("L1", Xbox360Button.LeftShoulder), ("R1", Xbox360Button.RightShoulder),
        ("Start", Xbox360Button.Start), ("Select", Xbox360Button.Back),
        ("Home", Xbox360Button.Guide),
        ("Up", Xbox360Button.Up), ("Down", Xbox360Button.Down),
        ("Left", Xbox360Button.Left), ("Right", Xbox360Button.Right),
        ("L3", Xbox360Button.LeftThumb), ("R3", Xbox360Button.RightThumb)
    ];

    private readonly ViGEmClient _client = new();
    private readonly IXbox360Controller _controller;

    public ViGEmVirtualController()
    {
        _controller = _client.CreateXbox360Controller();
        _controller.Connect();
    }

    public void Apply(InputPacket packet)
    {
        if (!string.Equals(packet.Type, "input", StringComparison.Ordinal))
            throw new InvalidDataException("Unsupported packet type.");

        var pressed = packet.Buttons.ToHashSet(StringComparer.OrdinalIgnoreCase);
        foreach (var (name, button) in Buttons)
            _controller.SetButtonState(button, pressed.Contains(name));

        _controller.SetAxisValue(Xbox360Axis.LeftThumbX, ToAxis(packet.LeftStick.X));
        _controller.SetAxisValue(Xbox360Axis.LeftThumbY, ToAxis(packet.LeftStick.Y));
        _controller.SetAxisValue(Xbox360Axis.RightThumbX, ToAxis(packet.RightStick.X));
        _controller.SetAxisValue(Xbox360Axis.RightThumbY, ToAxis(packet.RightStick.Y));
        _controller.SetSliderValue(Xbox360Slider.LeftTrigger, ToTrigger(packet.Triggers.L2));
        _controller.SetSliderValue(Xbox360Slider.RightTrigger, ToTrigger(packet.Triggers.R2));
        _controller.SubmitReport();
    }

    public void Reset() => Apply(new InputPacket { Type = "input" });

    public void Dispose()
    {
        _controller.Disconnect();
        _client.Dispose();
    }

    private static short ToAxis(float value) => (short)Math.Round(Math.Clamp(value, -1f, 1f) * short.MaxValue);
    private static byte ToTrigger(float value) => (byte)Math.Round(Math.Clamp(value, 0f, 1f) * byte.MaxValue);
}
