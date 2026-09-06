package net.minecraft.server.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

public class MinimalModeListener extends ServerGamePacketListenerImpl {

    public MinimalModeListener(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }

    private boolean containsInvalidValues(double x, double y, double z, float yRot, float xRot) {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yRot) || !Float.isFinite(xRot);
    }

    @Override
    public void handleMovePlayer(final ServerboundMovePlayerPacket packet) {
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());

        if (containsInvalidValues(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), packet.getYRot(0.0F), packet.getXRot(0.0F))) {
            this.disconnect(net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.invalid_player_movement"), org.bukkit.event.player.PlayerKickEvent.Cause.INVALID_PLAYER_MOVEMENT);
            return;
        }

        double oldX = this.player.getX();
        double oldY = this.player.getY();
        double oldZ = this.player.getZ();
        float oldYRot = this.player.getYRot();
        float oldXRot = this.player.getXRot();

        double newX = packet.getX(oldX);
        double newY = packet.getY(oldY);
        double newZ = packet.getZ(oldZ);
        float newYRot = packet.getYRot(oldYRot);
        float newXRot = packet.getXRot(oldXRot);

        if (oldX == newX && oldY == newY && oldZ == newZ && oldYRot == newYRot && oldXRot == newXRot) {
            return; // No actual movement
        }

        Location from = new Location(this.player.level().getWorld(), oldX, oldY, oldZ, oldYRot, oldXRot);
        Location to = new Location(this.player.level().getWorld(), newX, newY, newZ, newYRot, newXRot);

        PlayerMoveEvent event = new PlayerMoveEvent(this.player.getBukkitEntity(), from, to);
        this.server.server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            this.teleport(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch());
            return;
        }

        if (!to.equals(event.getTo()) && event.getTo() != null) {
            this.teleport(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch());
            return;
        }

        this.player.setPos(newX, newY, newZ);
        this.player.setYRot(newYRot);
        this.player.setXRot(newXRot);
        this.player.setOnGround(packet.isOnGround());

    }

    @Override public void handlePlayerInput(final ServerboundPlayerInputPacket packet) {}
    @Override public void handleMoveVehicle(final ServerboundMoveVehiclePacket packet) {}
    @Override public void handleAcceptTeleportPacket(final ServerboundAcceptTeleportationPacket packet) {}
    @Override public void handleAcceptPlayerLoad(final ServerboundPlayerLoadedPacket packet) {}
    @Override public void handleRecipeBookSeenRecipePacket(final ServerboundRecipeBookSeenRecipePacket packet) {}
    @Override public void handleBundleItemSelectedPacket(final ServerboundSelectBundleItemPacket packet) {}
    @Override public void handleRecipeBookChangeSettingsPacket(final ServerboundRecipeBookChangeSettingsPacket packet) {}
    @Override public void handleSeenAdvancements(final ServerboundSeenAdvancementsPacket packet) {}
    @Override public void handleCustomCommandSuggestions(final ServerboundCommandSuggestionPacket packet) {}
    @Override public void handleSetCommandBlock(final ServerboundSetCommandBlockPacket packet) {}
    @Override public void handleSetCommandMinecart(final ServerboundSetCommandMinecartPacket packet) {}
    @Override public void handlePickItemFromBlock(final ServerboundPickItemFromBlockPacket packet) {}
    @Override public void handlePickItemFromEntity(final ServerboundPickItemFromEntityPacket packet) {}
    @Override public void handleRenameItem(final ServerboundRenameItemPacket packet) {}
    @Override public void handleSetBeaconPacket(final ServerboundSetBeaconPacket packet) {}
    @Override public void handleSetGameRule(final ServerboundSetGameRulePacket packet) {}
    @Override public void handleSetStructureBlock(final ServerboundSetStructureBlockPacket packet) {}
    @Override public void handleSetTestBlock(final ServerboundSetTestBlockPacket packet) {}
    @Override public void handleTestInstanceBlockAction(final ServerboundTestInstanceBlockActionPacket packet) {}
    @Override public void handleSetJigsawBlock(final ServerboundSetJigsawBlockPacket packet) {}
    @Override public void handleJigsawGenerate(final ServerboundJigsawGeneratePacket packet) {}
    @Override public void handleSelectTrade(final ServerboundSelectTradePacket packet) {}
    @Override public void handleEditBook(final ServerboundEditBookPacket packet) {}
    @Override public void handleEntityTagQuery(final ServerboundEntityTagQueryPacket packet) {}
    @Override public void handleContainerSlotStateChanged(final ServerboundContainerSlotStateChangedPacket packet) {}
    @Override public void handleBlockEntityTagQuery(final ServerboundBlockEntityTagQueryPacket packet) {}
    @Override public void handlePlayerAction(final ServerboundPlayerActionPacket packet) {}
    @Override public void handleUseItemOn(final ServerboundUseItemOnPacket packet) {}
    @Override public void handleUseItem(final ServerboundUseItemPacket packet) {}
    @Override public void handleTeleportToEntityPacket(final ServerboundTeleportToEntityPacket packet) {}
    @Override public void handlePaddleBoat(final ServerboundPaddleBoatPacket packet) {}
    @Override public void handlePlayerCommand(final ServerboundPlayerCommandPacket packet) {}
    @Override public void handleInteract(final ServerboundInteractPacket packet) {}
    @Override public void handleClientCommand(final ServerboundClientCommandPacket packet) {}
    @Override public void handleContainerClose(final ServerboundContainerClosePacket packet) {}
    @Override public void handleContainerClick(final ServerboundContainerClickPacket packet) {}
    @Override public void handlePlaceRecipe(final ServerboundPlaceRecipePacket packet) {}
    @Override public void handleContainerButtonClick(final ServerboundContainerButtonClickPacket packet) {}
    @Override public void handleSetCreativeModeSlot(final ServerboundSetCreativeModeSlotPacket packet) {}
    @Override public void handleSignUpdate(final ServerboundSignUpdatePacket packet) {}
    @Override public void handlePlayerAbilities(final ServerboundPlayerAbilitiesPacket packet) {}
    @Override public void handleClientInformation(final ServerboundClientInformationPacket packet) { super.handleClientInformation(packet); }
    @Override public void handleCustomPayload(final ServerboundCustomPayloadPacket packet) { super.handleCustomPayload(packet); }
    @Override public void handleChangeDifficulty(final ServerboundChangeDifficultyPacket packet) {}
    @Override public void handleLockDifficulty(final ServerboundLockDifficultyPacket packet) {}
}
