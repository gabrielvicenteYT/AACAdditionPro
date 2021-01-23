package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.Dependency;
import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.util.files.configs.Configs;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class VersionControl implements Module, Dependency
{
    @Getter
    private static final VersionControl instance = new VersionControl();

    /**
     * Unmodifiable {@link Set} containing all registered {@link ProtocolVersion}s.
     */
    private static final Set<ProtocolVersion> PROTOCOL_VERSIONS = ImmutableSet.of(
            new ProtocolVersion("1.8", ServerVersion.MC188, 47),
            new ProtocolVersion("1.9", ServerVersion.MC19, 107, 108, 109, 110),
            new ProtocolVersion("1.10", ServerVersion.MC110, 210),
            new ProtocolVersion("1.11", ServerVersion.MC111, 315, 316),
            new ProtocolVersion("1.12", ServerVersion.MC112, 335, 338, 340),
            new ProtocolVersion("1.13", ServerVersion.MC113, 393, 401, 404),
            new ProtocolVersion("1.14", ServerVersion.MC114, 477, 480, 485, 490, 498),
            new ProtocolVersion("1.15", ServerVersion.MC115, 573, 575),
            new ProtocolVersion("1.16", ServerVersion.MC116, 735, 736, 751, 753));

    /**
     * Method used to get the {@link ServerVersion} from the protocol version number.
     *
     * @param protocolVersion the int returned by {@link us.myles.ViaVersion.api.ViaAPI#getPlayerVersion(Object)} or
     *                        {@link us.myles.ViaVersion.api.ViaAPI#getPlayerVersion(UUID)}
     */
    public static ServerVersion getServerVersionFromProtocolVersion(int protocolVersion)
    {
        for (ProtocolVersion version : PROTOCOL_VERSIONS) {
            if (version.versionNumbers.contains(protocolVersion)) {
                return version.equivalentServerVersion;
            }
        }
        return null;
    }

    @Override
    public void enable()
    {
        // Message:
        final Collection<String> versionStrings = new ArrayList<>();
        final List<Integer> blockedProtocolNumbers = new ArrayList<>();

        for (ProtocolVersion protocolVersion : PROTOCOL_VERSIONS) {
            if (protocolVersion.allowed) {
                versionStrings.add(protocolVersion.name);
            } else {
                // Set the blocked versions
                blockedProtocolNumbers.addAll(protocolVersion.versionNumbers);
            }
        }

        // Set the kick message.
        Configs.VIAVERSION.getConfigurationRepresentation().requestValueChange(
                "block-disconnect-msg",
                // Construct the message.
                Preconditions.checkNotNull(AACAdditionPro.getInstance().getConfig().getString("ClientControl.VersionControl.message"), "VersionControl message is null. Please fix your config.")
                             // Replace the special placeholder
                             .replace("{supportedVersions}", String.join(", ", versionStrings)));

        // Make the protocol numbers appear more visually appealing in the ViaVersion config
        blockedProtocolNumbers.sort(Integer::compareTo);

        // Block the affected protocol numbers.
        Configs.VIAVERSION.getConfigurationRepresentation().requestValueChange("block-protocols", blockedProtocolNumbers);
    }

    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public Set<String> getDependencies()
    {
        return ImmutableSet.of("ViaVersion");
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.VERSION_CONTROL;
    }

    /**
     * Key element for protocol versions.
     */
    @Getter
    private static class ProtocolVersion
    {
        /**
         * The name of the {@link ProtocolVersion}. Intended to be equivalent to minecraft versions.
         * Examples: 1_8, 1_9, 1_10, etc.
         */
        private final String name;

        /**
         * Whether or not this {@link ProtocolVersion} should be allowed to join the server.
         */
        private final boolean allowed;

        /**
         * What {@link ServerVersion} should be used when using this {@link ProtocolVersion}.
         */
        private final ServerVersion equivalentServerVersion;

        /**
         * An immutable {@link Set} of {@link Integer}s that contains all protocol version numbers associated with this {@link ProtocolVersion}
         */
        private final Set<Integer> versionNumbers;

        private ProtocolVersion(final String name, final ServerVersion equivalentServerVersion, final Integer... versionNumbers)
        {
            this.name = name;
            this.allowed = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl.allowedVersions." + this.name, true);
            this.equivalentServerVersion = equivalentServerVersion;
            this.versionNumbers = ImmutableSet.copyOf(versionNumbers);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProtocolVersion that = (ProtocolVersion) o;
            return equivalentServerVersion == that.equivalentServerVersion;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(equivalentServerVersion);
        }
    }
}