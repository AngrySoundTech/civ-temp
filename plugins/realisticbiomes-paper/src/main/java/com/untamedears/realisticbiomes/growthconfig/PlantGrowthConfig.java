package com.untamedears.realisticbiomes.growthconfig;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;

import com.untamedears.realisticbiomes.RealisticBiomes;
import com.untamedears.realisticbiomes.growth.IArtificialGrower;
import com.untamedears.realisticbiomes.growthconfig.inner.BiomeGrowthConfig;
import com.untamedears.realisticbiomes.growthconfig.inner.PersistentGrowthConfig;
import com.untamedears.realisticbiomes.model.Plant;
import com.untamedears.realisticbiomes.utils.RBUtils;

import vg.civcraft.mc.civmodcore.api.BlockAPI;
import vg.civcraft.mc.civmodcore.api.ItemNames;
import vg.civcraft.mc.civmodcore.util.TextUtil;

public class PlantGrowthConfig extends AbstractGrowthConfig {

	private static Random rng = new Random();
	private static final byte MAX_LIGHT = 15;
	private static final long INFINITE_TIME = TimeUnit.DAYS.toMillis(365L * 1000L);
	private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

	private ItemStack item;
	private short id;

	private List<Material> applicableVanillaPlants;

	private Map<Material, Double> greenHouseRates;

	private int maximumSoilLayers;
	private double maximumSoilBonus;
	private Map<Material, Double> soilBoniPerLevel;

	private boolean allowBoneMeal;
	private boolean needsLight;

	private BiomeGrowthConfig biomeGrowthConfig;
	private IArtificialGrower grower;
	private boolean canBePlantedDirectly;

	public PlantGrowthConfig(String name, short id, ItemStack item, Map<Material, Double> greenHouseRates,
			Map<Material, Double> soilBoniPerLevel, int maximumSoilLayers, double maximumSoilBonus,
			boolean allowBoneMeal, BiomeGrowthConfig biomeGrowthConfig, boolean needsLight, IArtificialGrower grower,
			List<Material> applicableVanillaPlants, boolean canBePlantedDirectly) {
		super(name);
		this.id = id;
		this.item = item;
		this.greenHouseRates = greenHouseRates;
		this.soilBoniPerLevel = soilBoniPerLevel;
		this.maximumSoilLayers = maximumSoilLayers;
		this.maximumSoilBonus = maximumSoilBonus;
		this.allowBoneMeal = allowBoneMeal;
		this.biomeGrowthConfig = biomeGrowthConfig;
		this.grower = grower;
		this.needsLight = needsLight;
		this.canBePlantedDirectly = canBePlantedDirectly;
		this.applicableVanillaPlants = applicableVanillaPlants;
	}

	/**
	 * Gets an information string to show to as growth information at a specific
	 * block
	 * 
	 * @param b Block to get growth information for
	 * @return Info string ready for output to a player
	 */
	public String getInfoString(Block b) {
		double soilMultiplier = 1.0 + getSoilBonus(b);
		double lightMultiplier = getLightMultiplier(b);
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.GOLD);
		sb.append(ItemNames.getItemName(item));
		if (biomeGrowthConfig instanceof PersistentGrowthConfig) {
			long time = getPersistentGrowthTime(b);
			if (time == -1) {
				sb.append(" does not grow here");
				return sb.toString();
			} else {
				sb.append(" will grow here within " + TextUtil.formatDuration(time, TimeUnit.MILLISECONDS));
			}
		} else {
			double baseMultiplier = biomeGrowthConfig.getNaturalProgressChance(b.getBiome());
			double totalMultiplier = baseMultiplier * soilMultiplier * lightMultiplier;
			if (totalMultiplier == 0) {
				sb.append(" does not grow here");
				return sb.toString();
			} else {
				sb.append(" has a ");
				sb.append(decimalFormat.format(totalMultiplier * 100));
				sb.append(" % chance to grow here");
			}
		}
		sb.append("\n");
		if (soilMultiplier != 1.0) {
			sb.append(ChatColor.AQUA + "Soil multiplier: " + soilMultiplier);
			sb.append("\n");
		}
		if (lightMultiplier != 1.0) {
			sb.append(ChatColor.GOLD + "Light multiplier: " + lightMultiplier);
		}
		return sb.toString();
	}

	public short getID() {
		return id;
	}

	public List<Material> getApplicableVanillaPlants() {
		return applicableVanillaPlants;
	}

	/**
	 * Multiplier to apply to the plants growth based on surrounding light
	 * conditions
	 * 
	 * @param block Block to check light conditions for
	 * @return Multplier to apply to growth rate
	 */
	private double getLightMultiplier(Block block) {
		if (!needsLight) {
			return 1.0;
		}
		byte naturalLight = block.getLightFromSky();
		if (naturalLight == MAX_LIGHT) {
			return 1.0;
		}
		double naturalRate = Math.pow((double) naturalLight / (double) MAX_LIGHT, 2);
		double greenHouseRate = 0.0;
		if (block.getLightFromBlocks() >= 12) {
			for (BlockFace face : BlockAPI.ALL_SIDES) {
				Block adjacent = block.getRelative(face);
				Double multiplier = greenHouseRates.get(adjacent.getType());
				if (multiplier != null) {
					greenHouseRate = Math.max(greenHouseRate, multiplier);
				}
			}
		}
		return Math.max(greenHouseRate, naturalRate);
	}

	/**
	 * @return Item/Plant for which this config applies
	 */
	public ItemStack getItem() {
		return item;
	}

	public  Map<Material, Double> getGreenHouseRates() {
		return greenHouseRates;
	}

	public int getMaximumSoilLayers() {
		return maximumSoilLayers;
	}

	public double getMaximumSoilBonus() {
		return maximumSoilBonus;
	}

	public Map<Material, Double> getSoilBoniPerLevel() {
		return soilBoniPerLevel;
	}

	public boolean getNeedsLight() {
		return needsLight;
	}

	public boolean getAllowBoneMeal() {
		return allowBoneMeal;
	}

	public BiomeGrowthConfig getBiomeGrowthConfig() {
		return biomeGrowthConfig;
	}
	
	public IArtificialGrower getGrower() {
		return grower;
	}
	
	/**
	 * @return Whether a plant of this config can be created by placing its matching item down. Will be false for melons or pumpkins for example
	 */
	public boolean canBePlantedDirectly() {
		return canBePlantedDirectly;
	}

	/**
	 * Looks at all surrounding factors to calculate how long a plant would take to
	 * grow at the given block with persistent growth
	 * 
	 * @param block Block to check growth condition for
	 * @return Total milli seconds needed to fully grow a plant
	 */
	public long getPersistentGrowthTime(Block block) {
		if (!biomeGrowthConfig.canGrowIn(block.getBiome())) {
			return -1;
		}
		double baseTime = ((PersistentGrowthConfig) biomeGrowthConfig).getTotalGrowthTimeNeeded(block.getBiome());
		baseTime /= (1.0 + getSoilBonus(block));
		double lightMultiplier = getLightMultiplier(block);
		if (lightMultiplier == 0.0) {
			baseTime = INFINITE_TIME;
		} else {
			baseTime /= lightMultiplier;
		}
		return (long) baseTime;
	}

	/**
	 * Gets an info string regarding the growth progress of a single plant
	 * 
	 * @param block Block at which the plant is
	 * @param plant Plant growing there, may be null for non-persistent growth
	 * @return Info string describing the plants growth progress
	 */
	public String getPlantInfoString(Block block, Plant plant) {
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.GOLD);
		sb.append(this.name);
		if (plant == null) {
			// non-persistent growth
			double progress = grower.getProgressGrowthStage(new Plant(block.getLocation(), this));
			sb.append(" is ");
			sb.append(decimalFormat.format(progress * 100));
			sb.append(" % grown");
		} else {
			if (isFullyGrown(plant)) {
				sb.append(" is fully grown ");
				return sb.toString();
			}
			long totalTime = getPersistentGrowthTime(block);
			long passedTime = System.currentTimeMillis() - plant.getCreationTime();
			long timeRemaining = Math.max(0, totalTime - passedTime);
			if (timeRemaining >= INFINITE_TIME) {
				sb.append(" will never grow here");
			} else {
				sb.append(" will grow to full size ");
				if (timeRemaining <= 0) {
					sb.append("now");
				} else {
					sb.append("in ");
					sb.append(TextUtil.formatDuration(timeRemaining, TimeUnit.MILLISECONDS));
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Calculates the soil bonus multiplier for the given plant at the given
	 * location. A soil bonus multiplier of 0.0 means no bonus and a growth rate of
	 * 1.0 for the plant, a soil bonus of 1.0 means a 2.0 multiplier for the plant
	 * etc.
	 * 
	 * @param block Block where the plant is
	 * @return Soil bonus applied for the location
	 */
	private double getSoilBonus(Block block) {
		Block soilBlock = block.getRelative(0, RBUtils.getVerticalSoilOffset(block.getType()), 0);
		double totalRate = 0.0;
		for (int i = 0; i < maximumSoilLayers; i++) {
			Double blockRate = soilBoniPerLevel.get(soilBlock.getType());
			if (blockRate == null) {
				break;
			}
			totalRate += blockRate;
			soilBlock = soilBlock.getRelative(BlockFace.DOWN);
		}
		return Math.min(totalRate, maximumSoilBonus);
	}

	/**
	 * Called when natural growth attempts to grow the plant. If the plant is
	 * persistent the event will always be cancelled, if not the growth succeeds
	 * with the configured chance
	 * 
	 * @param event Growth event happening
	 */
	public void handleAttemptedGrowth(Cancellable event, Block block) {
		double chance = biomeGrowthConfig.getNaturalProgressChance(block.getBiome());
		if (chance == 0) {
			event.setCancelled(true);
			return;
		}
		chance *= (1.0 + getSoilBonus(block));
		chance *= getLightMultiplier(block);
		if (rng.nextDouble() > chance) {
			event.setCancelled(true);
		}
	}

	public boolean isBonemealAllowed() {
		return allowBoneMeal;
	}

	/**
	 * Checks whether the given plant is fully grown
	 * 
	 * @param block Block of the plant
	 * @return True if the plant has reached its maximum growth stage, false
	 *         otherwise
	 */
	public boolean isFullyGrown(Plant plant) {
		return grower.getMaxStage() == grower.getStage(plant);
	}

	public boolean isPersistent() {
		return biomeGrowthConfig instanceof PersistentGrowthConfig;
	}
	
	public long updatePlant(Plant plant) {
		Block block = plant.getLocation().getBlock();
		return updatePlant(plant, block);
	}

	/**
	 * Updates the world state of the plant to match its intended state based on its
	 * creation time and calculates the next time stamp at which the plant should be
	 * updated
	 * 
	 * @param plant Plant to update
	 * @param block Block the plant is at
	 * @return UNIX time stamp at which the plant needs to be updated next if it is
	 *         still growing or Long.MAX_VALUE if it will never grow or if it is already
	 *         fully grown
	 */
	public long updatePlant(Plant plant, Block block) {
		if (plant.getGrowthConfig() == null) {
			plant.setGrowthConfig(this);
		}
		if (plant.getGrowthConfig() != this) {
			throw new IllegalStateException("Can not grow plant with different growth config, at " + plant.getLocation()
					+ " with " + plant.getGrowthConfig().getName() + ", but this is " + getName());
		}
		if (!biomeGrowthConfig.canGrowIn(block.getBiome())) {
			return Long.MAX_VALUE;
		}
		long totalTime = getPersistentGrowthTime(block);
		if (totalTime == -1) {
			return Long.MAX_VALUE;
		}
		long creationTime = plant.getCreationTime();
		long now = System.currentTimeMillis();
		long timeElapsed = now - creationTime;
		double progress = (double) timeElapsed / (double) totalTime;
		int intendedState = Math.min((int) (grower.getMaxStage() * progress), grower.getMaxStage());
		if (intendedState != grower.getStage(plant)) {
			try {
				grower.setStage(plant, intendedState);
			} catch (IllegalArgumentException e) {
				RealisticBiomes.getInstance().getLogger().warning("Failed to update stage for " + block.toString());
				//delete
				plant.getOwningCache().remove(plant);
				return Long.MAX_VALUE;
			}
			if (intendedState != grower.getStage(plant)) {
				//setting the state failed due to some external condition, we assume this wont change any time soon
				return Long.MAX_VALUE;
			}
		}
		if (plant.getGrowthConfig() != this) {
			//happens for example when a stem fully grows
			return plant.getGrowthConfig().updatePlant(plant, block);
		}

		if (intendedState == grower.getMaxStage() && grower.deleteOnFullGrowth()) {
			plant.getOwningCache().remove(plant);
			return Long.MAX_VALUE;
		}
		double incPerStage = grower.getIncrementPerStage();
		double nextProgressStage = (intendedState + incPerStage) / grower.getMaxStage();
		nextProgressStage = Math.min(nextProgressStage, 1.0);
		long timeFromCreationTillNextStage = (long) (totalTime * nextProgressStage);
		return creationTime + timeFromCreationTillNextStage;
	}
	
	public String toString() {
		return getName();
	}

}
