using System.Buffers;
using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using GamePadX.Server;

var token = Convert.ToHexString(RandomNumberGenerator.GetBytes(16)).ToLowerInvariant();
var port = int.TryParse(Environment.GetEnvironmentVariable("GAMEPADX_PORT"), out var configuredPort) ? configuredPort : 8765;
var localAddress = Dns.GetHostAddresses(Dns.GetHostName())
    .FirstOrDefault(address => address.AddressFamily == AddressFamily.InterNetwork && IsPrivateAddress(address));
if (localAddress is null)
    throw new InvalidOperationException("No private IPv4 address was found. Connect this PC to a private Wi-Fi network and retry.");

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.UseUrls($"http://{localAddress}:{port}");
var jsonOptions = new JsonSerializerOptions(JsonSerializerDefaults.Web);
var connectionGate = new SemaphoreSlim(1, 1);

using var controller = new ViGEmVirtualController();
var app = builder.Build();
app.UseWebSockets();

Console.WriteLine("GamePadX PC server ready.");
Console.WriteLine($"Pairing token: {token}");
Console.WriteLine($"Connect from the phone to ws://{localAddress}:{port}/ws?token=<pairing-token>");
Console.WriteLine("Keep this token private. Stop the server with Ctrl+C.");

app.MapGet("/", () => Results.Ok(new { name = "GamePadX", status = "ready" }));
app.Map("/ws", async (HttpContext context) =>
{
    var suppliedToken = context.Request.Query["token"].ToString();
    var suppliedBytes = Encoding.UTF8.GetBytes(suppliedToken);
    var expectedBytes = Encoding.UTF8.GetBytes(token);
    if (suppliedBytes.Length != expectedBytes.Length || !CryptographicOperations.FixedTimeEquals(suppliedBytes, expectedBytes))
    {
        context.Response.StatusCode = StatusCodes.Status401Unauthorized;
        return;
    }

    if (!context.WebSockets.IsWebSocketRequest)
    {
        context.Response.StatusCode = StatusCodes.Status400BadRequest;
        return;
    }

    if (!connectionGate.Wait(0))
    {
        context.Response.StatusCode = StatusCodes.Status409Conflict;
        return;
    }

    WebSocket socket;
    try
    {
        socket = await context.WebSockets.AcceptWebSocketAsync();
    }
    catch
    {
        connectionGate.Release();
        throw;
    }

    using (socket)
    {
    Console.WriteLine($"Controller connected from {context.Connection.RemoteIpAddress}.");
    var buffer = ArrayPool<byte>.Shared.Rent(4096);
    try
    {
        while (socket.State == WebSocketState.Open)
        {
            using var message = new MemoryStream();
            WebSocketReceiveResult result;
            do
            {
                result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), context.RequestAborted);
                if (result.MessageType == WebSocketMessageType.Close)
                    break;
                if (result.MessageType != WebSocketMessageType.Text || message.Length + result.Count > 4096)
                    throw new InvalidDataException("Only text input packets up to 4 KiB are accepted.");
                message.Write(buffer, 0, result.Count);
            } while (!result.EndOfMessage);

            if (result.MessageType == WebSocketMessageType.Close)
                break;

            var packet = JsonSerializer.Deserialize<InputPacket>(message.ToArray(), jsonOptions);
            if (packet is null || packet.Buttons.Length > 16 || packet.Timestamp <= 0)
                throw new InvalidDataException("Malformed input packet.");
            if (packet.Type == "ping")
            {
                var pong = JsonSerializer.SerializeToUtf8Bytes(new { type = "pong", timestamp = packet.Timestamp });
                await socket.SendAsync(new ArraySegment<byte>(pong), WebSocketMessageType.Text, true, context.RequestAborted);
                continue;
            }
            controller.Apply(packet);
        }
    }
    catch (OperationCanceledException) when (context.RequestAborted.IsCancellationRequested)
    {
    }
    catch (WebSocketException)
    {
    }
    catch (Exception exception) when (exception is InvalidDataException or JsonException)
    {
        Console.WriteLine($"Rejected client packet: {exception.Message}");
        if (socket.State == WebSocketState.Open)
            await socket.CloseAsync(WebSocketCloseStatus.InvalidPayloadData, "Invalid input packet", CancellationToken.None);
    }
    finally
    {
        controller.Reset();
        connectionGate.Release();
        ArrayPool<byte>.Shared.Return(buffer);
        Console.WriteLine("Controller disconnected.");
    }
    }
});

await app.RunAsync();

static bool IsPrivateAddress(IPAddress address)
{
    var bytes = address.GetAddressBytes();
    return bytes[0] == 10 || bytes[0] == 192 && bytes[1] == 168 || bytes[0] == 172 && bytes[1] is >= 16 and <= 31;
}
