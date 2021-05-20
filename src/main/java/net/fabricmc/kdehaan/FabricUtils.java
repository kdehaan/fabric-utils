package net.fabricmc.kdehaan;


import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class FabricUtils implements ClientModInitializer {

	public static Logger LOGGER = LogManager.getLogger();
	private static KeyBinding FIND_KEYBIND;
	private static KeyBinding GLOW_KEYBIND;
	public static final String MOD_ID = "fabric-utils";
	private long lastPressed = System.currentTimeMillis();
	private boolean glowEnabled = false;
	private MinecraftClient mc = MinecraftClient.getInstance();

	private class SearchType {
		Block blocktype;
		Field field;
		SearchType(Block blocktype, Field field) {
			field.setAccessible(true);
			this.blocktype = blocktype;
			this.field = field;
		}
	}


	public HashMap<String, SearchType> blockMappings = new HashMap() {{
		try {
			put("diamond ore", new SearchType(Blocks.DIAMOND_ORE, FabricUtilConfig.class.getDeclaredField("diamond")));
			put("ancient debris", new SearchType(Blocks.ANCIENT_DEBRIS, FabricUtilConfig.class.getDeclaredField("netherite")));
			put("gold ore", new SearchType(Blocks.GOLD_ORE, FabricUtilConfig.class.getDeclaredField("gold")));
			put("iron ore", new SearchType(Blocks.IRON_ORE, FabricUtilConfig.class.getDeclaredField("iron")));
			put("lapis lazuli", new SearchType(Blocks.LAPIS_ORE, FabricUtilConfig.class.getDeclaredField("lapis")));
			put("redstone ore", new SearchType(Blocks.REDSTONE_ORE, FabricUtilConfig.class.getDeclaredField("redstone")));
			put("emerald ore", new SearchType(Blocks.EMERALD_ORE, FabricUtilConfig.class.getDeclaredField("emerald")));
			put("monster spawner", new SearchType(Blocks.SPAWNER, FabricUtilConfig.class.getDeclaredField("spawner")));
			put("quartz ore", new SearchType(Blocks.NETHER_QUARTZ_ORE, FabricUtilConfig.class.getDeclaredField("quartz")));
			put("end portal frame", new SearchType(Blocks.END_PORTAL_FRAME,FabricUtilConfig.class.getDeclaredField("portalframe")));
		}catch(Exception e) {
			System.out.println("Field does not exist");
		}
	}};



	@Override
	public void onInitializeClient() {
		LOGGER.info("Finder initialized");
		FIND_KEYBIND = new KeyBinding("find", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "fabric-utils");
		GLOW_KEYBIND = new KeyBinding("glow", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "fabric-utils");
		KeyBindingHelper.registerKeyBinding(FIND_KEYBIND);
		KeyBindingHelper.registerKeyBinding(GLOW_KEYBIND);
		AutoConfig.register(FabricUtilConfig.class, GsonConfigSerializer::new);
		FabricUtilConfig config = AutoConfig.getConfigHolder(FabricUtilConfig.class).getConfig();


		ClientTickEvents.END_CLIENT_TICK.register(e ->
		{
			if(FIND_KEYBIND.isPressed()){
				if ((System.currentTimeMillis() - lastPressed) > 200) {

					lastPressed = System.currentTimeMillis();

					BlockPos pos = mc.player.getBlockPos();
					blockMappings.forEach((k, v) -> handleMappings(k, v, pos, config));

				}
			}
			if(GLOW_KEYBIND.isPressed()){
				if ((System.currentTimeMillis() - lastPressed) > 200) {
					lastPressed = System.currentTimeMillis();
					glowEnabled = !glowEnabled;
					glowNearbyEntities(glowEnabled);
				}
			}
			if ((System.currentTimeMillis() - lastPressed) > config.glowRefreshTime) {
				if (glowEnabled) {
					glowNearbyEntities(true);
				}
			}
		});
	}

	private void glowNearbyEntities(boolean enabled) {
		try {
			Iterable<Entity> nearbyEntities = mc.player.clientWorld.getEntities();
			for (Entity target: nearbyEntities) {
				target.setGlowing(enabled);
			}
		} catch (Exception e) {
			System.out.println("Error making entities glow");
		}

	}

	private void handleMappings(String key, SearchType value, BlockPos playerpos, FabricUtilConfig config) {
		try {
			if ((boolean)value.field.get(config)) {
				printFilteredOutput(getDirection(key, findNearestBlock(value.blocktype, playerpos, config)), config.actionBar);
			}
		} catch (Exception e) {
			System.out.println("Illegal Access");
		}


	}

	private int getCardinalFacing(float yaw) {
		int dir = (int) Math.floor((double)(yaw * 4.0F / 360.0F) + 0.5D);
		return dir & 3;
	}

	private void printFilteredOutput(String message, Boolean actionBar) {
		if (!message.isEmpty()) {
			assert mc.player != null;
			mc.player.sendMessage(new LiteralText(message), actionBar);
		}
	}

	private String getDirection(String blockType, BlockPos block) {
		int dir = getCardinalFacing(mc.player.yaw);
		BlockPos playerPos = mc.player.getBlockPos();
		if (getManhattanDistance(block, playerPos) < 1) {
			return "";
		}
		int xDist = block.getX() - playerPos.getX();
		int yDist = block.getY() - playerPos.getY();
		int zDist = block.getZ() - playerPos.getZ();
		switch (dir) {
			case 0:
				return  "The closest " + blockType + " is " + Math.abs(zDist) + " blocks " + (zDist > 0 ? "forward" : "backward") +
						", " + Math.abs(xDist) + " blocks " + (xDist > 0 ? "left" : "right") + " and " +  Math.abs(yDist) +
						" blocks " + (yDist > 0 ? "up" : "down");
			case 2:
				return  "The closest " + blockType + " is " + Math.abs(zDist) + " blocks " + (zDist < 0 ? "forward" : "backward") +
						", " + Math.abs(xDist) + " blocks " + (xDist < 0 ? "left" : "right") + " and " +  Math.abs(yDist) +
						" blocks " + (yDist > 0 ? "up" : "down");

			case 1:
				return  "The closest " + blockType + " is " + Math.abs(xDist) + " blocks " + (xDist < 0 ? "forward" : "backward") +
						", " + Math.abs(zDist) + " blocks " + (zDist > 0 ? "left" : "right") + " and " +  Math.abs(yDist) +
						" blocks " + (yDist > 0 ? "up" : "down");
			case 3:
				return  "The closest " + blockType + " is " + Math.abs(xDist) + " blocks " + (xDist > 0 ? "forward" : "backward") +
						", " + Math.abs(zDist) + " blocks " + (zDist < 0 ? "left" : "right") + " and " +  Math.abs(yDist) +
						" blocks " + (yDist > 0 ? "up" : "down");
			default:
				return blockType + " not found";
		}

	}


	private BlockPos findNearestBlock(Block blockID, BlockPos playerPos, FabricUtilConfig config) {
		ArrayList<BlockPos> foundBlocks = findBlocks(blockID, playerPos, config);
		int dist = Integer.MAX_VALUE;
		BlockPos closest = playerPos;
		for (int i = 0; i < foundBlocks.size(); i++){
			BlockPos block = foundBlocks.get(i);
			int temp = getManhattanDistance(block, playerPos);
			if (temp < dist) {
				dist = temp;
				closest = block;
			}
		}
		return closest;
	}

	private int getManhattanDistance(BlockPos pos1, BlockPos pos2) {
		int dist = 0;
		dist += Math.abs(pos1.getX() - pos2.getX());
		dist += Math.abs(pos1.getY() - pos2.getY());
		dist += Math.abs(pos1.getZ() - pos2.getZ());
		return dist;
	}

	private ArrayList<BlockPos> findBlocks(Block blockID,  BlockPos playerPos, FabricUtilConfig config) {
		int zxRange = config.xzRange;
		int yRange = config.yRange;
		ArrayList<BlockPos> resultList = new ArrayList<>();
		int playerY = playerPos.getY();
		int playerX = playerPos.getX();
		int playerZ = playerPos.getZ();
		for (int yIter = playerY - yRange; yIter <= playerY + yRange + 1; ++yIter) {
			for (int zIter = playerZ - zxRange; zIter <= playerZ + zxRange; ++zIter) {
				for (int xIter = playerX - zxRange; xIter <= playerX + zxRange; ++xIter) {
					BlockState state = mc.world.getBlockState(new BlockPos(xIter, yIter, zIter));
					if (state.getBlock() == blockID) {
						BlockPos foundBlock = new BlockPos(xIter, yIter, zIter);
						resultList.add(foundBlock);
					}
					Thread.yield();
				}
			}
		}
//		System.out.println(playerPos);
		return resultList;
	}


}
