package com.untamedears.realisticbiomes.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.bukkit.World;
import org.bukkit.block.Block;

import com.avaje.ebeaninternal.server.lib.sql.DataSourceException;
import com.untamedears.realisticbiomes.GrowthConfig;
import com.untamedears.realisticbiomes.RealisticBiomes;

public class PlantChunk {
	RealisticBiomes plugin;

	HashMap<Coords, Plant> plants;

	// index of this chunk in the database
	long index;

	boolean loaded;
	boolean inDatabase;

	public PlantChunk(RealisticBiomes plugin, Connection readConn, long index) {
		this.plugin = plugin;
		plants = new HashMap<Coords, Plant>();
		this.index = index;

		this.loaded = false;
		this.inDatabase = false;

	}

	/**
	 * tostring override
	 */
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("<PlantChunk: index: " + index);
		sb.append(" loaded: " + loaded);
		sb.append(" inDatabase: " + inDatabase);
		sb.append(" plants: \n");

		if (this.plants != null) {
			for (Coords iterCoords : plants.keySet()) {

				sb.append("\tPlantHashmapEntry[coords: " + iterCoords);
				sb.append(" = plant: " + plants.get(iterCoords));
				sb.append(" ] \n");

			}
		}

		sb.append(" > ");
		return sb.toString();
	}

	// /-------------------------

	public synchronized boolean isLoaded() {
		return loaded;
	}

	public synchronized int getPlantCount() {

		if (plants == null) {
			RealisticBiomes.LOG
					.severe("PLANTS HASHMAP IS NULL, THIS SHOULD NOT HAPPEN");
			return 0;
		}
		return plants.keySet().size();
	}

	// /-------------------------

	public synchronized void remove(Coords coords) {
		if (!loaded)
			return;

		plants.remove(coords);
	}

	public synchronized void add(Coords coords, Plant plant,
			Connection writeConn) {

		RealisticBiomes.LOG.finer("plantchunk.add(): called with coords: "
				+ coords + " and plant " + plant);
		RealisticBiomes.LOG.finer("plantchunk.add(): is loaded? " + loaded);
		if (!loaded) {

			load(coords, writeConn);

			loaded = true;
		}

		plants.put(coords, plant);
	}

	public synchronized Plant get(Coords coords) {
		if (!loaded) {
			RealisticBiomes.LOG
					.finer("Plantchunk.get(): returning null cause not loaded");
			return null;
		}
		return plants.get(coords);
	}

	/**
	 * Loads the plants from the database into this PlantChunk object.
	 * 
	 * @param coords
	 * @param readConn
	 * @return
	 */
	public synchronized boolean load(Coords coords, Connection readConn) {
		// if the data is being loaded, it is known that this chunk is in the
		// database

		// TODO: plant chunk objects need to know their own coordinates, we
		// should
		// not be passing them in to load / unload!

		plugin.getLogger().finer(
				"Plantchunk.load() called with coords: " + coords);

		if (loaded) {
			RealisticBiomes.LOG
					.finer("Plantchunk.load(): plant chunk is already loaded, returning true");
			return true;
		}

		World world = plugin.getServer().getWorld(WorldID.getMCID(coords.w));
		PreparedStatement loadPlantsStmt;

		// TODO: put this with the other sql prepared statements
		try {
			loadPlantsStmt = readConn
					.prepareStatement(String
							.format("SELECT w, x, y, z, date, growth FROM %s_plant WHERE chunkid = ?",
									this.plugin.persistConfig.prefix));

		} catch (SQLException e) {
			throw new DataSourceException(
					"Failed to create prepared statement in PlantChunk.load()",
					e);
		}

		// execute the load plant statement
		try {

			loadPlantsStmt.setLong(1, index);
			plugin.getLogger().finer(
					"PlantChunk.load() executing sql query: "
							+ loadPlantsStmt.toString());
			loadPlantsStmt.execute();

			ResultSet rs = loadPlantsStmt.getResultSet();
			while (rs.next()) {
				int w = rs.getInt("w");
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				int z = rs.getInt("z");
				long date = rs.getLong(5);
				float growth = rs.getFloat(6);

				plugin.getLogger()
						.finer(String
								.format("PlantChunk.load(): got result: w:%s x:%s y:%s z:%s date:%s growth:%s",
										w, x, y, z, date, growth));

				// if the plant does not correspond to an actual crop, don't
				// load it
				if (!plugin.getGrowthConfigs().containsKey(
						world.getBlockAt(x, y, z).getType())) {
					plugin.getLogger()
							.finer("Plantchunk.load(): plant we got from db doesn't correspond to an actual crop, not loading");
					continue;
				}

				Plant plant = new Plant(date, growth);

				// TODO MARK: this code seems very similar to
				// RealisticBiomes.growAndPersistBlock()
				// grow the block
				Block block = world.getBlockAt(x, y, z);
				GrowthConfig growthConfig = plugin.getGrowthConfigs().get(
						block.getType());
				double growthAmount = growthConfig.getRate(block)
						* plant.setUpdateTime(System.currentTimeMillis() / 1000L);
				plant.addGrowth((float) growthAmount);

				// and update the plant growth
				plugin.getBlockGrower().growBlock(block, coords,
						plant.getGrowth());

				// if the plant isn't finished growing, add it to the
				// plants
				if (!(plant.getGrowth() >= 1.0)) {
					plants.put(new Coords(w, x, y, z), plant);
					RealisticBiomes.LOG
							.finer("PlantChunk.load(): plant not finished growing, adding to plants list");
				}

				// END MARK TODO
			}

			loadPlantsStmt.close();
		} catch (SQLException e) {
			throw new DataSourceException(
					String.format(
							"Failed to execute/load the data from the plants table (In PlantChunk.load()) with chunkId %s, coords %s",
							index, coords), e);
		}

		// TODO: this always returns true...refactor that!
		loaded = true;
		return true;
	}

	/**
	 * unloads the plant chunk, and saves it to the database.
	 * 
	 * Note that this is called by PlantManager.saveAllAndStop(), so that method
	 * takes care of setting autocommit to false/true and actually committing to
	 * the database
	 * 
	 * @param chunkCoords
	 * @param writeStmts
	 */
	public synchronized void unload(Coords chunkCoords) {

		RealisticBiomes.LOG.finest("PlantChunk.unload(): called with coords "
				+ chunkCoords + "plantchunk object: " + this);
		// TODO: plant chunk objects need to know their own coordinates, we
		// should
		// not be passing them in to load / unload!
		if (!loaded)
			return;

		try {
			// if this chunk was not in the database, then add it to the
			// database
			RealisticBiomes.LOG.finer("PlantChunk.unload(): is inDatabase?: "
					+ inDatabase);
			if (!inDatabase) {

				RealisticBiomes.LOG.finer("not in database, adding new chunk");
				ChunkWriter.addChunkStmt.setInt(1, chunkCoords.w);
				ChunkWriter.addChunkStmt.setInt(2, chunkCoords.x);
				ChunkWriter.addChunkStmt.setInt(3, chunkCoords.z);
				ChunkWriter.addChunkStmt.execute();
				ChunkWriter.getLastChunkIdStmt.execute();
				ResultSet rs = ChunkWriter.getLastChunkIdStmt.getResultSet();

				// need to call rs.next() to get the first result, and make sure
				// we get the index, and throw an exception
				// if we don't
				if (rs.next()) {
					index = rs.getLong(1);
					RealisticBiomes.LOG
							.finer("plantchunk.unload(): got new autoincrement index, it is now "
									+ index);
				} else {
					throw new DataSourceException(
							"Trying to add the chunk to the database, but was unable to get "
									+ "the last inserted statement to get the index");
				}

				// make sure to commit this newly added chunk, or else when we
				// add plants to the
				// database later in this method we dont get a Constraint
				// Failure exception

				RealisticBiomes.LOG
						.finer("plantchunk.unload(): committing new plantchunk with index "
								+ this.index);
				ChunkWriter.writeConnection.commit();
				inDatabase = true;
			}

		} catch (SQLException e) {

			throw new DataSourceException(
					String.format(
							"Failed to unload the chunk (In PlantChunk, adding chunk to db if needed), index %s, coords %s, PlantChunk obj: %s",
							index, chunkCoords, this), e);
		}

		try {
			// put all the plants into the database
			// if we are already unloaded then don't do anything
			if (loaded) {
				if (!plants.isEmpty()) {

					// delete plants in the database for this chunk and re-add them
					// this is OK because rb_plant does not have a autoincrement index
					// so it won't explode. However, does this have a negative performance impact?
					// TODO: add listener for block break event, and if its a plant, we remove it
					// from the correct plantchunk? Right now if a plant gets destroyed before
					// it is fully grown then it won't get remove from the database
					ChunkWriter.deleteOldPlantsStmt.setLong(1, index);
					ChunkWriter.deleteOldPlantsStmt.execute();

					for (Coords coords : plants.keySet()) {
						Plant plant = plants.get(coords);

						ChunkWriter.addPlantStmt.clearParameters();
						ChunkWriter.addPlantStmt.setLong(1, index);
						ChunkWriter.addPlantStmt.setInt(2, coords.w);
						ChunkWriter.addPlantStmt.setInt(3, coords.x);
						ChunkWriter.addPlantStmt.setInt(4, coords.y);
						ChunkWriter.addPlantStmt.setInt(5, coords.z);
						ChunkWriter.addPlantStmt.setLong(6,
								plant.getUpdateTime());
						ChunkWriter.addPlantStmt.setFloat(7,
								plant.getGrowth());
						RealisticBiomes.LOG
								.finest("PlantChunk.unload(): executing add plant statement: "
										+ ChunkWriter.addPlantStmt);
						ChunkWriter.addPlantStmt.execute();

					} // end for
				} 
			}
		} catch (SQLException e) {
			throw new DataSourceException(
					String.format(
							"Failed to unload the chunk (In PlantChunk, "
									+ "replacing with new data/deleting), index %s, coords %s, PlantChunk obj: %s",
							index, chunkCoords, this), e);
		}

		plants = new HashMap<Coords, Plant>();
		loaded = false;
	}

}
