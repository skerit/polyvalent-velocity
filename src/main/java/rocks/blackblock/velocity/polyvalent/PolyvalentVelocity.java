package rocks.blackblock.velocity.polyvalent;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
        id = "@ID@",
        name = "@NAME@",
        description = "@DESCRIPTION@",
        authors = {"skerit"},
        version = "@VERSION@" // filled in during build
)
public class PolyvalentVelocity {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Class.forName("com.velocitypowered.api.proxy.LoginPhaseConnection");
        } catch (ClassNotFoundException e) {
            logger.error("You need to use Velocity 3.1.0 or above to use the Polyvalent adapter.");
            return;
        }

        server.getEventManager().register(this, new PolyvalentVelocityListener());
    }

}
