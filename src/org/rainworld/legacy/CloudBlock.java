package org.rainworld.legacy;
import static org.rainworld.RainUtil.isAir;
import static org.rainworld.RainUtil.isCloud;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.rainworld.RainUtil;

public class CloudBlock {
  static final int MAX_RAIN_DELAY = 50;
  static final int RAIN_FADE_DELAY = 50;
  static final int DROPLET_DIVISIONS = 3;
  static final String METADATA_KEY = "cloud";

  protected Cloud cloud;
  protected int id;

  // Grow Cloud
  public CloudBlock(Cloud cloud, Location offset) {
    this.cloud = cloud;
  }

  // Origin Cloud
  public CloudBlock(Cloud cloud) {
    this(cloud, new Location(RainUtil.getOverworld(), 0, 0, 0));
    id = cloud.count;
    cloud.count++;
  }

  // ACTIONS
  //

  public boolean draw() {
    Location loc = getLoc();

    // move cloud
    if (cloud.moved) {
      destroy(lastLoc());
      if (!placeBlock(loc)) return false;
    }

    // animate rain
    if (RainUtil.aboveAir(loc)) {
      SpawnDroplet(loc);
    }

    return true;
  }

  // WORLD INTERACTIONS
  //

  private boolean placeBlock(Location loc) {
    Block cloudBlock = loc.getBlock();
    if (RainUtil.isCloud(cloudBlock) || !RainUtil.isAir(cloudBlock)) {
      return false;
    }

    cloudBlock.setType(Material.WHITE_WOOL);
    cloudBlock.setMetadata(METADATA_KEY, new FixedMetadataValue(cloud.plugin, this));
    return true;
  }

  public void destroy() {
    destroy(lastLoc());
  }

  public void destroy(Location loc) {
    Block block = loc.getBlock();
    if (!RainUtil.blockIsCloud(block, this)) return;
    block.removeMetadata(METADATA_KEY, cloud.plugin);
    //RainUtil.debug(loc);
    block.setType(Material.AIR);
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
        cursorLoc.add((divX + 0.5) / DROPLET_DIVISIONS, 0, (divZ + 0.5) / DROPLET_DIVISIONS);

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
    return cloud.loc.clone();
  }

  private Location lastLoc() {
    Location lastLoc = getLoc();
    lastLoc.subtract(cloud.delta);
    return lastLoc;
  }

  /**
   * Find adjacent unclouded neighbors 
   * @param factors grow parameters of the cloud
   * @return locs mapped to global positions
   */
  public Map<Location, Double> freeNeighbors(Map<String, Double> factors) {
    Map<Location, Double> neighbors = new HashMap<Location, Double>();
    Location loc = getLoc();

    for (int y = -1; y <= 1; y++) { // then move up to next layer
      for (int z = -1; z <= 1; z++) { // then add z axis, completing pane ^
        for (int x = -1; x <= 1; x++) { // x axis first ^
          Location neighborOffset = loc.clone();
          neighborOffset.add(x, y, z);
          // capture available
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

  public boolean equals(CloudBlock other) {
    return cloud.equals(other.cloud) && (id == other.id);
  }
}
