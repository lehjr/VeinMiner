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

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.*;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import portablejim.veinminer.configuration.ConfigurationValues;
import portablejim.veinminer.core.MinerInstance;
import portablejim.veinminer.event.EntityDropHook;
import portablejim.veinminer.lib.ModInfo;
import portablejim.veinminer.network.ConnectionHandler;
import portablejim.veinminer.network.PacketHandler;
import portablejim.veinminer.proxy.CommonProxy;
import portablejim.veinminer.server.MinerCommand;
import portablejim.veinminer.server.MinerServer;
import portablejim.veinminer.util.BlockID;

/**
 * This class is the main mod class for Veinminer. It is loaded as a mod
 * through ForgeModLoader.
 */

@Mod(modid = ModInfo.MOD_ID, name = ModInfo.MOD_NAME, version = ModInfo.VERSION)
@NetworkMod(clientSideRequired = false, serverSideRequired = false, channels = { ModInfo.CHANNEL },
        packetHandler = PacketHandler.class, connectionHandler = ConnectionHandler.class)
public class VeinMiner {

    ConfigurationValues configurationValues;

    @Instance(ModInfo.MOD_ID)
    public static VeinMiner instance;

    @SidedProxy(clientSide = ModInfo.PROXY_CLIENT_CLASS, serverSide = ModInfo.PROXY_SERVER_CLASS)
    public static CommonProxy proxy;

    @PreInit
    public void preInit(FMLPreInitializationEvent event) {
        configurationValues = new ConfigurationValues(event.getSuggestedConfigurationFile());
        proxy.setupConfig(configurationValues);
        proxy.registerKeybind();

        LanguageRegistry.instance().loadLocalization("/assets/veinminer/lang/en_US.lang", "en_US", false);
    }

    @Init
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new EntityDropHook());

    }

    @PostInit
    public void postInit(FMLPostInitializationEvent event) {

    }

    @Mod.ServerStarting
    public void serverStarting(FMLServerStartingEvent event) {
    }

    @ServerStarted
    public void serverStarted(FMLServerStartedEvent event) {
        new MinerServer(configurationValues);

        ServerCommandManager serverCommandManger = (ServerCommandManager) MinecraftServer.getServer().getCommandManager();
        serverCommandManger.registerCommand(new MinerCommand());
    }

    public void blockMined(World world, EntityPlayerMP player, int x, int y, int z, boolean harvestBlockSuccess, BlockID blockId) {
        String output = String.format("Block mined at %d,%d,%d, result %b, block id is %d:%d", x, y, z, harvestBlockSuccess, blockId.id, blockId.metadata);
        MinerInstance ins = new MinerInstance(world, player, x, y, z, blockId, MinerServer.instance);
        ins.mineVein(x, y, z);
        FMLLog.getLogger().info(output);
    }
}
