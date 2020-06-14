package org.rainworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import java.lang.Math;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author lxk1170
 */
public class Cloud {
    // const
    public final static int MAX_SIZE = 50;
    public final static long SPAWN_TIME = 200;
    public final static long DRAW_TIME = 5;
    public final static long TICK_TIME = 15;
    
    public final static double DEFAULT_THRESHOLD = 0.7;
    public final static double DEFAULT_TOP = 0.8;
    public final static double DEFAULT_BOTTOM = 0.2;
    public final static double DEFAULT_SIDES = 1.0;
    public final static double DEFAULT_CORNERS = 0.2;
    public static int CLOUD_COUNT = 0;
    
    
    // vars
    protected Plugin plugin;
    protected Player creator;
    protected List<CloudBlock> cloudBlocks;
    protected List<CloudBlock> newCloudBlocks;
    protected boolean spawning;
    protected Location loc;   // (origin block) cloud location
    protected Location delta; // movement delta since last draw
    protected boolean moved;
    protected int id;
    protected int count;
    private double speed;
    private int direction;
    
    private List<BukkitTask> tasks;
    private Map<String, Double> growFactors = new HashMap() {{
        put("threshold", DEFAULT_THRESHOLD);
        put("top", DEFAULT_TOP);
        put("bottom", DEFAULT_BOTTOM);
        put("sides", DEFAULT_SIDES);
        put("corners", DEFAULT_CORNERS);
    }};
    
    
    public Cloud(Plugin plugin, Player player, Map<String, Double> factors) {
       	this(plugin, RainUtil.cloudAbove(player));
	this.creator = player;
        if (factors != null) { growFactors.putAll(factors); }
    }
    
    public Cloud(Plugin plugin, Location origin) {
        this.plugin = plugin;
        
        cloudBlocks = new ArrayList();
        newCloudBlocks = new ArrayList();

	count = 0;	
	id = CLOUD_COUNT;
	CLOUD_COUNT++;

        loc = origin.getBlock().getLocation().clone();
        delta = new Location(origin.getWorld(), 0, 0, 0);
        spawning = true;

	int direction = ThreadLocalRandom.current().nextInt(0, 360);
	double speed = Math.sqrt(Math.random());
	setDirection(direction, speed);
        
	moved = true; // moved into existance, thus draw
        
        // add the start block
        newCloudBlocks.add(new CloudBlock(this));
        
        tasks = new ArrayList();                                                // threads          
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        tasks.add(scheduler.runTaskTimer(plugin, () -> tick(), 0l, TICK_TIME));       // tick
        tasks.add(scheduler.runTaskTimer(plugin, () -> draw(), 20l, DRAW_TIME));      // draw
        tasks.add(scheduler.runTaskLater(plugin, () -> { spawning = false; }, SPAWN_TIME)); // stop spawning
    }
    
    public void draw() {
	while (newCloudBlocks.size() > 0) {
	    cloudBlocks.add(newCloudBlocks.remove(0));
	}

	// draw blocks, or destroy
	for (int i=0; i<cloudBlocks.size(); i++) {
	    if (!cloudBlocks.get(i).draw()) {
		cloudBlocks.remove(i).destroy();
	    }
	}
        
        // reset delta
	if (moved) {
	    delta.setX(0);
	    delta.setY(0);
            delta.setZ(0);
	    moved = false;
	}
    }
        
    public void tick() {
        move();
        
        if (spawning) {
            spawn();
        }
    }
    
    private void notify(String message) {
    	if (creator != null) {
	    creator.sendMessage(message);
	}
    }
     
    // spawn more cloudblocks
    private void spawn() {
        Map<Location, Double> freeBlocks = new HashMap();
        Double threshold = growFactors.get("threshold");
        
        // collect all free spaces, removing duplicates
        for (CloudBlock cloudBlock : cloudBlocks) {
            Map<Location, Double> neighbors = cloudBlock.freeNeighbors(growFactors);
            freeBlocks.putAll(neighbors);
        }
        
        // try to spawn on free space
        freeBlocks.forEach((neighbor, factor) -> {
            int cloudHeight = (neighbor.getBlockY() - RainUtil.getGroundAt(neighbor).getBlockY());
            int blockY = loc.getBlockY()+neighbor.getBlockY();

            // roll dice
	    double result = RainUtil.rollDice(blockY, cloudHeight, factor);
            if (result > threshold) {
                if (cloudBlocks.size() < MAX_SIZE) {
                    newCloudBlocks.add(new CloudBlock(this, neighbor));
	    	    notify("Cloud grew");
                } else {
	    	    notify("Max cloud size reached: " + MAX_SIZE);
		}
            }
        });
        
    }
    
    /**
    * Set the clouds movement
    * @param direction 0-360 degrees
    * @param speed how much to move in the direction
    */
    public void setDirection(int direction, double speed) {
        this.direction = direction;
        this.speed = speed;
    }
   
    /**
     * @return a location representing the movement delta
     */
    private void move() {
        // calculate next tick movement delta
        double piDirection = (direction/360.0)*2.0*Math.PI;
        double xMove = Math.cos(piDirection) * speed;
        double zMove = Math.sin(piDirection) * speed;
        Location tickDelta = new Location(RainUtil.getOverworld(), xMove, 0, zMove);
        
        // add change in movement
        loc.add(tickDelta);   // position in world
        delta.add(tickDelta); // position relative to last draw
        
        if (hasMoved()) {
	    moved = true;
	}
    }

    // destroy all cloudblocks
    public void destroy() {
        while(tasks.size() > 0) {
            tasks.remove(0).cancel();
        }
        
        while(cloudBlocks.size() > 0) {
            cloudBlocks.remove(0).destroy();
        }
    }
    

    // INFO
    //

    public boolean hasMoved() {
        return (Math.abs(delta.getX()) >= 1) || (Math.abs(delta.getY()) >= 1) || (Math.abs(delta.getZ()) >= 1);
    }

    @Override
    public String toString() {
        return "{loc: "+loc+", ms: "+speed+", dir: "+direction+", spwn: "+spawning+"}";
    }
}
