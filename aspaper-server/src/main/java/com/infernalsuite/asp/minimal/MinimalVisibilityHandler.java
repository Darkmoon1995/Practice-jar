package com.infernalsuite.asp.minimal;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.util.Mth;

public class MinimalVisibilityHandler implements Listener {

    private boolean isMinimalMode(org.bukkit.World world) {
        try {
            java.lang.reflect.Method method = org.bukkit.World.class.getMethod("isMinimalMode");
            return (boolean) method.invoke(world);
        } catch (Exception e) {
            return false;
        }
    }

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new MinimalVisibilityHandler(), new com.infernalsuite.asp.InternalPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!isMinimalMode(player.getWorld())) return;

        handleSpawn(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isMinimalMode(player.getWorld())) return;

        handleMove(player, event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        boolean wasMinimal = isMinimalMode(event.getFrom().getWorld());
        boolean isMinimal = isMinimalMode(event.getTo().getWorld());

        if (wasMinimal && !isMinimal) {
            handleDespawn(player);
        } else if (!wasMinimal && isMinimal) {
            handleSpawn(player);
        } else if (isMinimal) {
            handleTeleport(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isMinimalMode(player.getWorld())) {
            handleDespawn(player);
        }
    }

    private void handleSpawn(Player player) {
        ServerPlayer sp = ((CraftPlayer) player).getHandle();

        ClientboundPlayerInfoUpdatePacket addInfoPacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(java.util.List.of(sp));
        ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(sp, 0, sp.blockPosition());

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            ServerPlayer otherSp = ((CraftPlayer) other).getHandle();

            otherSp.connection.send(addInfoPacket);
            otherSp.connection.send(spawnPacket);
            otherSp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(sp.getId(), sp.getEntityData().getNonDefaultValues()));

            ClientboundPlayerInfoUpdatePacket infoOther = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(java.util.List.of(otherSp));
            ClientboundAddEntityPacket spawnOther = new ClientboundAddEntityPacket(otherSp, 0, otherSp.blockPosition());
            sp.connection.send(infoOther);
            sp.connection.send(spawnOther);
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(otherSp.getId(), otherSp.getEntityData().getNonDefaultValues()));
        }
    }

    private void handleDespawn(Player player) {
        ServerPlayer sp = ((CraftPlayer) player).getHandle();
        ClientboundRemoveEntitiesPacket removePacket = new ClientboundRemoveEntitiesPacket(sp.getId());

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            ServerPlayer otherSp = ((CraftPlayer) other).getHandle();
            otherSp.connection.send(removePacket);
        }
    }

    private void handleMove(Player player, org.bukkit.Location from, org.bukkit.Location to) {
        if (to == null) return;

        ServerPlayer sp = ((CraftPlayer) player).getHandle();
long dx = (long) ((to.getX() - from.getX()) * 4096.0D);
        long dy = (long) ((to.getY() - from.getY()) * 4096.0D);
        long dz = (long) ((to.getZ() - from.getZ()) * 4096.0D);

        byte yRot = (byte) Mth.floor(to.getYaw() * 256.0F / 360.0F);
        byte xRot = (byte) Mth.floor(to.getPitch() * 256.0F / 360.0F);

        if (dx < Short.MIN_VALUE || dx > Short.MAX_VALUE || dy < Short.MIN_VALUE || dy > Short.MAX_VALUE || dz < Short.MIN_VALUE || dz > Short.MAX_VALUE) {
            handleTeleport(player);
            return;
        }

        ClientboundMoveEntityPacket.PosRot packet = new ClientboundMoveEntityPacket.PosRot(
                sp.getId(),
                (short) dx, (short) dy, (short) dz,
                yRot, xRot,
                sp.onGround()
        );
        ClientboundRotateHeadPacket headPacket = new ClientboundRotateHeadPacket(sp, yRot);

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            ServerPlayer otherSp = ((CraftPlayer) other).getHandle();
            otherSp.connection.send(packet);
            otherSp.connection.send(headPacket);
        }
    }

    private void handleTeleport(Player player) {
        ServerPlayer sp = ((CraftPlayer) player).getHandle();
        net.minecraft.world.entity.PositionMoveRotation pos = net.minecraft.world.entity.PositionMoveRotation.of(sp);
        net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket tpPacket = new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(sp.getId(), pos, java.util.Set.of(), sp.onGround());

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            ServerPlayer otherSp = ((CraftPlayer) other).getHandle();
            otherSp.connection.send(tpPacket);
        }
    }
}
