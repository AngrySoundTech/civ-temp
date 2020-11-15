package com.untamedears.realisticbiomes.growth;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.untamedears.realisticbiomes.model.Plant;

import vg.civcraft.mc.civmodcore.api.BlockAPI;
import vg.civcraft.mc.civmodcore.api.MaterialAPI;

public class VerticalGrower extends IArtificialGrower {
	
	public static Block getRelativeBlock(Block block, BlockFace face) {
		Material mat = block.getType();
		Block bottomBlock = block;
		// not actually using this variable, but just having it here as a fail safe
		for (int i = 0; i < 257; i++) {
			Block below = bottomBlock.getRelative(face);
			if (below.getType() != mat) {
				break;
			}
			bottomBlock = below;
		}
		return bottomBlock;
	}

	private int maxHeight;
	private Material material;
	private boolean instaBreakTouching;
	private BlockFace primaryGrowthDirection;
	
	public VerticalGrower(int maxHeight, Material material, BlockFace primaryGrowthDirection, boolean instaBreakTouching) {
		this.maxHeight = maxHeight;
		this.material = material;
		this.instaBreakTouching = instaBreakTouching;
		this.primaryGrowthDirection = primaryGrowthDirection;
	}
	
	public Material getMaterial() {
		return material;
	}

	@Override
	public int getIncrementPerStage() {
		return 1;
	}
	
	public BlockFace getPrimaryGrowthDirection() {
		return primaryGrowthDirection;
	}

	@Override
	public int getMaxStage() {
		return maxHeight - 1;
	}

	@Override
	public int getStage(Plant plant) {
		Block block = plant.getLocation().getBlock();
		if (material != block.getType()) {
			return -1;
		}
		Block bottom = getRelativeBlock(block, primaryGrowthDirection.getOppositeFace());
		if (!bottom.getLocation().equals(block.getLocation())) {
			return -1;
		}
		return getActualHeight(block) - 1;
	}

	protected int getActualHeight(Block block) {
		Block bottom = getRelativeBlock(block, BlockFace.DOWN);
		Block top = getRelativeBlock(block, BlockFace.UP);

		return top.getY() - bottom.getY() + 1;
	}

	/**
	 * Handles the growth of a column plant ( i.e sugarcane, cactus )
	 * 
	 * @param block   Block of the corresponding plant
	 * @param howMany How tall should the growth be
	 * @return highest plant block
	 */
	protected Block growVertically(Plant plant, Block block, int howMany) {
		if (material != null && block.getType() != material) {
			block.setType(material);
		}

		int counter = 1;
		Block onTop = block;
		while (counter < maxHeight && howMany > 0) {
			counter++;
			onTop = onTop.getRelative(primaryGrowthDirection);
			Material topMaterial = onTop.getType();
			if (topMaterial == Material.AIR) {
				if (instaBreakTouching) {
					for(BlockFace face : BlockAPI.PLANAR_SIDES) {
						Block side = onTop.getRelative(face);
						if (!MaterialAPI.isAir(side.getType())) {
							ItemStack toDrop = plant.getGrowthConfig().getItem().clone();
							toDrop.setAmount(howMany);
							Location loc = block.getLocation();
							loc.add(0.5, 0.5, 0.5);
							Item item = block.getWorld().dropItemNaturally(loc, toDrop);
							item.setVelocity(item.getVelocity().multiply(1.2));
							plant.resetCreationTime();
							return onTop.getRelative(primaryGrowthDirection.getOppositeFace());
						}
					}
				}
				onTop.setType(material, true);
				howMany--;
				continue;
			}
			if (topMaterial == block.getType()) {
				// already existing block of the same plant
				continue;
			}
			// neither air, nor the right plant, but something else blocking growth, so we
			// stop
			break;
		}

		return onTop.getType() != material ? onTop.getRelative(primaryGrowthDirection.getOppositeFace()) : onTop;
	}

	@Override
	public void setStage(Plant plant, int stage) {
		int currentStage = getStage(plant);
		if (stage <= currentStage) {
			return;
		}
		Block block = plant.getLocation().getBlock();
		growVertically(plant, block, stage - currentStage);
	}


	@Override
	public boolean deleteOnFullGrowth() {
		return false;
	}

	public boolean ignoreGrowthFailure() {
		return instaBreakTouching;
	}
}
