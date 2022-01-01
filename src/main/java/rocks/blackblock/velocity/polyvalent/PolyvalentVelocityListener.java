package rocks.blackblock.velocity.polyvalent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.event.connection.DisconnectEvent;

import java.util.concurrent.TimeUnit;

public class PolyvalentVelocityListener {

    private static final MinecraftChannelIdentifier HANDSHAKE_CHANNEL = MinecraftChannelIdentifier.create("polyvalent", "handshakev2");
    private static final MinecraftChannelIdentifier ID_MAP_CHANNEL = MinecraftChannelIdentifier.create("polyvalent", "id_map");

    // This cache will store the handshake data for at most 10 seconds
    private final Cache<String, byte[]> handshakeByUsername = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    @Subscribe(order = PostOrder.LATE)
    public void onPreLogin(PreLoginEvent event) {

        if (!event.getResult().isAllowed()) {
            return;
        }

        if (event.getConnection().getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
            return;
        }

        LoginPhaseConnection connection = (LoginPhaseConnection) event.getConnection();

        // Send the initial empty handshake request to the client in Polyvalent's place
        connection.sendLoginPluginMessage(HANDSHAKE_CHANNEL, new byte[0], responseBody -> {

            // If the response is null, the client did not understand the request,
            // meaning it does not have Polyvalent installed.
            if (responseBody == null) {
                return;
            }

            // Remember the response to the handshake for later
            handshakeByUsername.put(event.getUsername(), responseBody);
        });
    }

    @Subscribe
    public void onServerLoginPluginMessage(ServerLoginPluginMessageEvent event) {

        ChannelIdentifier identifier = event.getIdentifier();
        ServerConnection connection = event.getConnection();
        Player player = connection.getPlayer();

        // The server is trying to send a handshake request to the client,
        // but that's OK because we probably already have the expected response in our cache!
        if (identifier.equals(HANDSHAKE_CHANNEL)) {

            byte[] handshake_response = handshakeByUsername.getIfPresent(player.getUsername());

            if (handshake_response != null) {
                event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(handshake_response));
            }

            return;
        }

        // The server is trying to send an ID_MAP to the client,
        // we just need to forward it to the client. It'll receive it as a ClientPlayConnectionEvents instead of a
        // ClientLoginNetworking one, but both have been added as a listener to client-side Polyvalent anyway
        if (identifier.equals(ID_MAP_CHANNEL)) {
            player.sendPluginMessage(ID_MAP_CHANNEL, event.getContents());
        }

    }
}
