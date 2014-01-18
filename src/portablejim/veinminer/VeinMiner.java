/* This file is part of VeinMiner.
 *
 *    VeinMiner is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation, either version 3 of
 *     the License, or (at your option) any later version.
 *
 *    VeinMiner is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with VeinMiner.
 *    If not, see <http://www.gnu.org/licenses/>.
 */

package portablejim.veinminer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.*;
import cpw.mods.fml.common.event.FMLFingerprintViolationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import net.minecraft.block.Block;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import portablejim.veinminer.api.VeinminerStartCheck;
import portablejim.veinminer.configuration.ConfigurationSettings;
import portablejim.veinminer.configuration.ConfigurationValues;
import portablejim.veinminer.core.MinerInstance;
import portablejim.veinminer.event.EntityDropHook;
import portablejim.veinminer.lib.Logger;
import portablejim.veinminer.lib.ModInfo;
import portablejim.veinminer.network.ConnectionHandler;
import portablejim.veinminer.network.PacketHandler;
import portablejim.veinminer.proxy.CommonProxy;
import portablejim.veinminer.server.MinerCommand;
import portablejim.veinminer.server.MinerServer;
import portablejim.veinminer.util.BlockID;

import java.util.ArrayList;
import java.util.Set;

import static portablejim.veinminer.configuration.ConfigurationSettings.ToolType;

/**
 * This class is the main mod class for Veinminer. It is loaded as a mod
 * through ForgeModLoader.
 */

@Mod(modid = ModInfo.MOD_ID,
        name = ModInfo.MOD_NAME,
        version = ModInfo.VERSION,
        acceptedMinecraftVersions = ModInfo.VALID_MC_VERSIONS,
        certificateFingerprint = "ad915af2d8bfa7bff330f4bb5a0a4551ef9e0aed")
@NetworkMod(clientSideRequired = false, serverSideRequired = false, channels = { ModInfo.CHANNEL },
        packetHandler = PacketHandler.class, connectionHandler = ConnectionHandler.class,
        versionBounds = "@@@DEV@@@")
public class VeinMiner {

    ConfigurationValues configurationValues;
    public ConfigurationSettings configurationSettings;

    @Metadata(value = ModInfo.MOD_ID)
    public static ModMetadata metadata;

    @Instance(ModInfo.MOD_ID)
    public static VeinMiner instance;

    @SidedProxy(clientSide = ModInfo.PROXY_CLIENT_CLASS, serverSide = ModInfo.PROXY_SERVER_CLASS)
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configurationValues = new ConfigurationValues(event.getSuggestedConfigurationFile());
        configurationSettings = new ConfigurationSettings(configurationValues);
        proxy.registerKeybind();

        metadata = event.getModMetadata();
        metadata.modId = ModInfo.MOD_ID;
        metadata.name = ModInfo.MOD_NAME;
        metadata.description = ModInfo.DESCRIPTION;
        metadata.version = ModInfo.VERSION;
        metadata.url = ModInfo.URL;
        metadata.updateUrl = ModInfo.UPDATE_URL;
        metadata.authorList = Lists.newArrayList(ModInfo.AUTHOR);
        metadata.credits = ModInfo.CREDITS;
        metadata.requiredMods = Sets.newHashSet((ArtifactVersion)new DefaultArtifactVersion("Forge", true));
        metadata.autogenerated = false; // Needed, otherwise will not work.
    }

    @EventHandler
    public void fingerprintWarning(FMLFingerprintViolationEvent event) {
        // Signing is just for updates. No crashes here.
        FMLLog.getLogger().warning(String.format("%s mod is not properly signed.", ModInfo.MOD_ID));
        FMLLog.getLogger().warning("This may be a copy somebody has modified, or it may be I just forgot to sign it myself.");
        FMLLog.getLogger().warning("Whatever the reason, it's probably ok.");
        FMLLog.getLogger().warning(String.format("Expected fingerprint: %s", event.expectedFingerprint));
    }

    @EventHandler
    public void init(@SuppressWarnings("UnusedParameters") FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new EntityDropHook());

        ModContainer thisMod = Loader.instance().getIndexedModList().get(ModInfo.MOD_ID);
        if(thisMod != null) {
            String fileName = thisMod.getSource().getName();
            if(fileName.contains("-dev") || !fileName.contains(".jar")) {
                ModInfo.DEBUG_MODE = true;
                Logger.debug("Enabling debug mode");
            }
        }
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        String[] oreDictList = OreDictionary.getOreNames();
        for(ToolType toolType : ToolType.values()) {
            Set<String> autodetectValues = configurationSettings.getAutodetectBlocksList(toolType);
            if(configurationSettings.getAutodetectBlocksToggle(toolType)) {
                for(String oreDictEntry : oreDictList) {
                    for(String autodetectValue : autodetectValues) {
                        if(!autodetectValue.isEmpty() && oreDictEntry.startsWith(autodetectValue)) {
                            ArrayList<ItemStack> itemStacks = OreDictionary.getOres(oreDictEntry);
                            for(ItemStack item : itemStacks) {
                                if(item.getItem() instanceof ItemBlock) {
                                    configurationSettings.addBlockToWhitelist(toolType, new BlockID(item.itemID, item.getItemDamage()));
                                    Logger.debug("Adding %d:%d (%s) to block whitelist for %s (%s:%s)", item.itemID, item.getItemDamage(), item.getDisplayName(), toolType.toString(), autodetectValue, oreDictEntry);
                                }
                            }
                        }
                    }
                }
            }
        }
        configurationSettings.saveConfigs();
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        new MinerServer(configurationValues);

        ServerCommandManager serverCommandManger = (ServerCommandManager) MinecraftServer.getServer().getCommandManager();
        serverCommandManger.registerCommand(new MinerCommand());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void blockMined(World world, EntityPlayerMP player, int x, int y, int z, boolean harvestBlockSuccess, BlockID blockId) {
         Logger.debug("Block mined at %d,%d,%d, result %b, block id is %d:%d", x, y, z, harvestBlockSuccess, blockId.id, blockId.metadata);

        if(blockId.id > Block.blocksList.length || Block.blocksList[blockId.id] == null  || !player.canHarvestBlock(Block.blocksList[blockId.id])) {
            return;
        }

        if(!harvestBlockSuccess) {
            VeinminerStartCheck startEvent = new VeinminerStartCheck(player, blockId.id, blockId.metadata);
            MinecraftForge.EVENT_BUS.post(startEvent);
            if(!startEvent.allowVeinminerStart) {
                return;
            }
        }

        MinerInstance ins = new MinerInstance(world, player, x, y, z, blockId, MinerServer.instance);
        ins.mineVein(x, y, z);
    }
}
