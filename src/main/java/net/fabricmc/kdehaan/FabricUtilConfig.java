package net.fabricmc.kdehaan;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.HashMap;

@Config(name = "fabric-utils")
public class FabricUtilConfig implements ConfigData {
    Boolean actionBar = false;
    int glowRefreshTime = 30000;
    int yRange = 5;
    int xzRange = 20;
    Boolean diamond = true;
    Boolean netherite = true;
    Boolean gold = false;
    Boolean iron = false;
    Boolean lapis = false;
    Boolean redstone = false;
    Boolean emerald = false;
    Boolean spawner = true;
    Boolean quartz = false;
    Boolean portalframe = true;


}