package de.photon.aacadditionpro.util.world;

import com.google.common.collect.Sets;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.util.exceptions.UnknownMinecraftVersion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Material;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaterialUtil
{
    public static final Set<Material> LIQUIDS;

    /**
     * Contains all containers that need a free space of any kind above the container (e.g. chests with a stair above)
     */
    public static final Set<Material> FREE_SPACE_CONTAINERS;
    public static final Set<Material> FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS;

    static {
        final EnumSet<Material> freeSpaceMaterials = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST);
        freeSpaceMaterials.addAll(getMaterialsEndingWith("SHULKER_BOK"));

        final EnumSet<Material> allowedMaterials = EnumSet.of(Material.AIR, Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.ANVIL);
        allowedMaterials.addAll(getMaterialsEndingWith("_SLAB", "_STAIRS"));

        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
                allowedMaterials.add(Material.getMaterial("ENCHANTMENT_TABLE"));

                LIQUIDS = Sets.immutableEnumSet(Material.WATER, Material.LAVA, Material.getMaterial("STATIONARY_WATER"), Material.getMaterial("STATIONARY_LAVA"));
                break;
            case MC113:
                allowedMaterials.add(Material.CAVE_AIR);
                allowedMaterials.add(Material.ENCHANTING_TABLE);

                LIQUIDS = Sets.immutableEnumSet(Material.WATER, Material.LAVA);
                break;
            case MC114:
            case MC115:
            case MC116:
                allowedMaterials.addAll(getMaterialsEndingWith("_SIGN"));

                allowedMaterials.add(Material.CAVE_AIR);
                allowedMaterials.add(Material.ENCHANTING_TABLE);

                LIQUIDS = Sets.immutableEnumSet(Material.WATER, Material.LAVA);
                break;
            default:
                throw new UnknownMinecraftVersion();
        }

        FREE_SPACE_CONTAINERS = Sets.immutableEnumSet(freeSpaceMaterials);
        FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS = Sets.immutableEnumSet(allowedMaterials);
    }

    public static Set<Material> getMaterialsEndingWith(String... ends)
    {
        final Set<Material> materials = EnumSet.noneOf(Material.class);
        for (Material material : Material.values()) {
            for (String end : ends) {
                if (material.name().endsWith(end)) {
                    materials.add(material);
                    break;
                }
            }
        }
        return materials;
    }

    /**
     * Fix for Spigot's broken occluding method.
     */
    public static boolean isReallyOccluding(Material material)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
                return material != Material.BARRIER && material != Material.getMaterial("MOB_SPAWNER") && material.isOccluding();
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                return material != Material.BARRIER && material != Material.SPAWNER && material.isOccluding();
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * Checks if a {@link Collection} of {@link Material}s contains certain {@link Material}s.
     */
    public static boolean containsMaterials(final Collection<Material> searchFor, final Collection<Material> toBeSearched)
    {
        for (Material material : searchFor) {
            if (toBeSearched.contains(material)) return true;
        }
        return false;
    }

    /**
     * Checks if a {@link Collection} of {@link Material}s contains liquids.
     * */
    public static boolean containsLiquids(final Collection<Material> toBeSearched)
    {
        return containsMaterials(LIQUIDS, toBeSearched);
    }
}