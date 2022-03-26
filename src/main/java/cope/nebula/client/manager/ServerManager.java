package cope.nebula.client.manager;

import cope.nebula.client.events.PacketEvent;
import cope.nebula.client.events.PacketEvent.Direction;
import cope.nebula.util.Globals;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.SPacketDisconnect;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.UUID;

/**
 * Manages things that do with the server.
 * Eg: ping for a player, TPS calculation, etc
 *
 * @see https://github.com/ionar2/spidermod/blob/master/src/main/java/me/ionar/salhack/managers/TickRateManager.java
 *
 * @author aesthetical
 * @since 3/26/22
 */
public class ServerManager implements Globals {
    private final float[] ticks = new float[20];
    private int currentTick = 0;
    private long lastTick = -1L;

    public ServerManager() {
        EVENT_BUS.register(this);

        Arrays.fill(ticks, 0.0f);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (event.getDirection().equals(Direction.INCOMING)) {
            if (event.getPacket() instanceof SPacketTimeUpdate) {
                if (lastTick != -1L) {
                    ticks[currentTick % ticks.length] = MathHelper.clamp(20.0f / ((System.currentTimeMillis() - lastTick) / 1000.0f), 0.0f, 20.0f);
                    ++currentTick;
                }

                lastTick = System.currentTimeMillis();
            }
        } else if (event.getPacket() instanceof SPacketDisconnect) {
            Arrays.fill(ticks, 0.0f);
            lastTick = -1L;
        }
    }

    /**
     * Gets the latency of a player
     * @param uuid The player's UUID
     * @return their response time, or -1 if none found.
     */
    public int getLatency(UUID uuid) {
        for (NetworkPlayerInfo info : mc.player.connection.getPlayerInfoMap()) {
            if (info.getGameProfile() != null && info.getGameProfile().getId().equals(uuid)) {
                return info.getResponseTime();
            }
        }

        return -1;
    }

    /**
     * Gets the servers TPS (ticks per second)
     *
     * Note: this is the average value
     *
     * @return the servers TPS
     */
    public float getTps() {
        float totalTickRate = 0.0f;
        int count = 0;

        for (float tick : ticks) {
            if (tick > 0.0f) {
                totalTickRate += tick;
                ++count;
            }
        }

        return MathHelper.clamp(totalTickRate / count, 0.0f, 20.0f);
    }
}