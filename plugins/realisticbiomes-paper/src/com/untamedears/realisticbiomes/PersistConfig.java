package com.untamedears.realisticbiomes;

public class PersistConfig {
	public String databaseName;
	
	// the period in tick at which data from unloaded chunks will be loaded into
	// the database
	public long unloadBatchPeriod;
	
	// the chance that a grow_event on a block will trigger a plant chunk load
	public double growEventLoadChance;
}