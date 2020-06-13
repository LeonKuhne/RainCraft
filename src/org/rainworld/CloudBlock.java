/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rainworld;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import static org.rainworld.RainUtil.isAir;
import static org.rainworld.RainUtil.isCloud;

/**
 * @author lxk1170
 */
public class CloudBlock {
    // const
    static final int MAX_RAIN_DELAY = 50;
    static final int RAIN_FADE_DELAY = 50;
    static final int DROPLET_DIVISIONS = 3;
    
    // vars
    private Cloud cloud;
    private Location offset;
    
    
    public CloudBlock(Cloud cloud, Location offset) {           // Grow Cloud
        this.cloud = cloud;
        this.offset = offset; // offset from center of cloud
    }
    
    public CloudBlock(Cloud cloud) {                                            // Origin Cloud
        this(cloud, new Location(RainUtil.getOverworld(), 0, 0, 0));
    }
    
    
    // ACTIONS
    //
    
    
    public void draw() {
        Location loc = getLoc();
        
        if (cloud.moved) {
            // place new and remove old cloud blocks from world
            if (!placeBlock()) { cloud.cloudBlocks.remove(this); }
            if (!RainUtil.sameBlock(lastLoc(), loc)) { destroy(); }
        }
        
        // animate rain
        if (!cloud.spawning) {
            if (RainUtil.aboveAir(loc)) {
                SpawnDroplet(loc);
            }
        }
    }
    
                
    // WORLD INTERACTIONS
    //
    
    private boolean placeBlock() {
        Block cloudBlock = getLoc().getBlock();
        if (!RainUtil.isCloud(cloudBlock) && RainUtil.isAir(cloudBlock)) {
            cloudBlock.setType(Material.WHITE_WOOL);
            cloudBlock.setMetadata("cloud", new FixedMetadataValue(cloud.plugin, true));
            return true;
        }
        
        return false;
    }
    
    public void destroy() {
        Location loc = lastLoc();
        Block cloudBlock = loc.getBlock();
        cloudBlock.removeMetadata("cloud", cloud.plugin);
        
        cloudBlock.setType(Material.AIR);
    }
    
    
    // RAIN
    //
    
    public static void SpawnDroplet(Location loc) {
        
        Location rainLoc = loc.clone();
        rainLoc.add(Math.random(), 0, Math.random());
        rainLoc.getWorld().spawnParticle(Particle.DRIP_WATER, rainLoc, 1);
    }
    
    public static void SpawnDropletDivisions(Plugin plugin, Location loc) {
        Location rainLoc = loc.clone();
        for (int divX = 0; divX < DROPLET_DIVISIONS; divX++) {
            for (int divZ = 0; divZ < DROPLET_DIVISIONS; divZ++) {
                Location cursorLoc = rainLoc.clone();
                cursorLoc.add((divX+0.5)/DROPLET_DIVISIONS, 0, (divZ+0.5)/DROPLET_DIVISIONS);

                // delay random amount
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
                    public void run() {
                        // spawn raindrop   
                        rainLoc.getWorld().spawnParticle(Particle.WATER_DROP, cursorLoc, 1);
                    }
                }, ThreadLocalRandom.current().nextInt(0, MAX_RAIN_DELAY));
            }
        }
    }
    
    
    // HELPER
    //
    
    private Location getLoc() {
        Location loc = cloud.loc.clone();
	loc.add(offset);
	return loc;
    }
    
    private Location lastLoc() {
	Location lastLoc = getLoc();
	lastLoc.subtract(cloud.delta);
	return lastLoc;
    }
    
    /**
     * Get the neighboring free air blocks next to this cloud block; checks all 8 surrounding blocks
     * @param factors control the grow parameters of the cloud; 
     * @return a map of locations containing offset (delta) information from the origin
     */
    public Map<Location, Double> freeNeighbors(Map<String, Double> factors) {
        Map<Location, Double> neighbors = new HashMap();
        Location loc = getLoc();
        
        // find neighbors
        for (int y=-1; y<=1; y++) {             //     then move up to next layer
            for (int z=-1; z<=1; z++) {         //   then add z axis, completing pane ^
                for (int x=-1; x<=1; x++) {     // x axis first ^
                    Location neighborOffset = offset.clone();
		    neighborOffset.add(x, y, z);
                    
                    // test if free
                    Location neighborWorld = neighborOffset.clone();
		    neighborWorld.add(loc);
                    if (isAir(neighborWorld) && !isCloud(neighborWorld)) {
                        Double factor = RainUtil.getFactor(factors, x, y, z);
                        neighbors.put(neighborOffset, factor);
                    }
                }
            }   
        }

        
        return neighbors;
    }
    
    
}
