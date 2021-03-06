package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.entity.EntityUtil;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.packetwrappers.client.IWrapperPlayClientLook;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.logging.Level.SEVERE;

/**
 * This {@link PacketListenerModule} checks for rotation packets which have
 * exactly the same yaw/pitch values as the last packet. When moving these values are never equal to each other when
 * sent by a vanilla client.
 */
class EqualRotationPattern extends PacketAdapter implements PacketListenerModule
{
    @Getter
    private static final EqualRotationPattern instance = new EqualRotationPattern();

    // A set of materials which hitboxes changed in minecraft 1.9
    private static final Set<Material> CHANGED_HITBOX_MATERIALS;

    static {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                CHANGED_HITBOX_MATERIALS = Collections.unmodifiableSet(EnumSet.of(Material.getMaterial("STAINED_GLASS_PANE"),
                                                                                  Material.getMaterial("THIN_GLASS"),
                                                                                  Material.getMaterial("IRON_FENCE"),
                                                                                  Material.CHEST,
                                                                                  Material.ANVIL));
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                // Hitbox bugs are fixed in higher versions.
                CHANGED_HITBOX_MATERIALS = Collections.emptySet();
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    EqualRotationPattern()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK);
    }


    @Override
    public void onPacketReceiving(final PacketEvent packetEvent)
    {
        final User user = UserManager.safeGetUserFromPacketEvent(packetEvent);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Get the packet.
        final IWrapperPlayClientLook lookWrapper = packetEvent::getPacket;

        final float currentYaw = lookWrapper.getYaw();
        final float currentPitch = lookWrapper.getPitch();

        // Boat false positive (usually worse cheats in vehicles as well)
        if (!user.getPlayer().isInsideVehicle() &&
            // Not recently teleported
            !user.hasTeleportedRecently(5000) &&
            // Same rotation values
            // LookPacketData automatically updates its values.
            currentYaw == user.getDataMap().getFloat(DataKey.PACKET_ANALYSIS_REAL_LAST_YAW) &&
            currentPitch == user.getDataMap().getFloat(DataKey.PACKET_ANALYSIS_REAL_LAST_PITCH) &&
            // Labymod fp when standing still / hit in corner fp
            user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 100))
        {
            // Not a big performance deal as most packets have already been filtered out, now we just account for
            // the last false positives.
            // Sync call because the isHitboxInLiquids method will load chunks (prevent errors).
            try {
                if (Boolean.TRUE.equals(Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () ->
                        // False positive when jumping from great heights into a pool with slime blocks on the bottom.
                        !(EntityUtil.isHitboxInLiquids(user.getPlayer().getLocation(), user.getHitbox()) &&
                          user.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.SLIME_BLOCK) &&
                        // Fixes false positives on versions 1.9+ because of changed hitboxes
                        !(ServerVersion.getActiveServerVersion() == ServerVersion.MC188 &&
                          ServerVersion.getClientServerVersion(user.getPlayer()) != ServerVersion.MC188 &&
                          EntityUtil.isHitboxInMaterials(user.getPlayer().getLocation(), user.getHitbox(), CHANGED_HITBOX_MATERIALS))).get(10, TimeUnit.SECONDS)))
                {
                    // Cancelled packets may cause problems.
                    if (user.getDataMap().getBoolean(DataKey.PACKET_ANALYSIS_EQUAL_ROTATION_EXPECTED)) {
                        user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_EQUAL_ROTATION_EXPECTED, false);
                        return;
                    }

                    PacketAnalysis.getInstance().getViolationLevelManagement().flag(user.getPlayer(),
                                                                                    -1, () -> {},
                                                                                    () -> VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent equal rotations."));
                }
            } catch (InterruptedException | ExecutionException e) {
                AACAdditionPro.getInstance().getLogger().log(SEVERE, "Unable to complete the EqualRotation calculations.", e);
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                AACAdditionPro.getInstance().getLogger().log(SEVERE, "Discard packet check due to high server load. If this message appears frequently please consider upgrading your server.");
            }
        }
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.EqualRotation";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
