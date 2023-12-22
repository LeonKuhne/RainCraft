package org.rainworld;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.rainworld.legacy.CloudBlock;

public class RainUtil {
  static Map<Integer, Integer> sessions = new HashMap<Integer, Integer>();
  private static Map<Double[], Double> cachedPoolTemperatures = new HashMap<Double[], Double>();
  private static Map<AbstractMap.SimpleEntry<Double, Double[]>, Double[]> cachedReversePoolTemperatures = new HashMap<AbstractMap.SimpleEntry<Double, Double[]>, Double[]>();

  // THREADING
  //

  private static void decrement(int sessionId) {
    sessions.put(sessionId, sessions.get(sessionId) - 1);
  }

  public static BukkitTask async(Runnable task) {
    return Rain.scheduler.runTaskAsynchronously(Rain.plugin, task);
  }

  public static <T> void iterAsync(List<T> items, Consumer<T> callback) {
    int sessionId = callback.hashCode();
    sessions.put(sessionId, items.size());
    for (T item : items) {
      async(() -> {
        callback.accept(item);
        callback.andThen((t) -> {
          decrement(callback.hashCode());
        });
      });
    }

    while (sessions.get(sessionId) > 0) {
      try { Thread.sleep(10); }
      catch (InterruptedException e) { e.printStackTrace(); }
    }
  }

  // BLOCKS
  //

  public static boolean blockIsCloud(Block block, CloudBlock otherBlock) {
    List<MetadataValue> meta = block.getMetadata("cloud");
    if (meta.size() == 0) {
      return false;
    }
    CloudBlock cloudBlock = (CloudBlock) meta.get(0).value();
    return cloudBlock.equals(otherBlock);
  }

  public static boolean sameLocation(Location locA, Location locB) {
    return (locA.getBlockX() == locB.getBlockX() &&
        locA.getBlockZ() == locB.getBlockZ() &&
        locA.getBlockY() == locB.getBlockY());
  }

  public static World getOverworld() {
    return Bukkit.getWorlds().get(0);
  }

  public static boolean isCloud(Block block) {
    return block.hasMetadata("cloud");
  }

  public static boolean isCloud(Location loc) {
    return isCloud(loc.getBlock());
  }

  public static boolean isAir(Block block) {
    return block.getType() == Material.AIR;
  }

  public static boolean isAir(Location loc) {
    return isAir(loc.getBlock());
  }

  public static Location getGroundAt(Location loc) {
    Location cursor = loc.getWorld().getHighestBlockAt(loc).getLocation().clone();

    // go down until not air (or cloud)
    while (cursor.getBlockY() > 0 && (isCloud(cursor) || isAir(cursor))) {
      cursor.add(0, -1, 0);
    }

    return cursor;
  }

  public static boolean aboveAir(Location loc) {
    Location below = loc.clone();
    below.add(0, -1, 0);
    return isAir(below);
  }

  public static Location cloudAbove(Player player) {
    return cloudAbove(player.getLocation());
  }

  public static Location cloudAbove(Location loc) {
    Location cloudLoc = loc.clone();
    int height = Rain.config.cloudHeight + (int) Math.round(Math.random() * Rain.config.cloudVariance);
    cloudLoc.add(0, height, 0);
    return cloudLoc;
  }

  // RANDOM
  //

  public static void debug(Location loc) {
    fill(loc, Material.REDSTONE_BLOCK);
  }

  public static void fill(Location loc, Material material) {
    loc.getBlock().setType(material);
  }

  public static double rollDice(double cloudHeight, double floorHeight, double extraFactor) {
    double percentWorldHeight = cloudHeight / (getOverworld().getMaxHeight());
    double percentCloudHeight = Math.abs(floorHeight / Rain.config.cloudHeight);

    // create factors
    double diceFactor = Math.random();
    double heightFactor = Math.pow(percentWorldHeight, 0.2);
    double floorFactor = Math.min(Math.pow(percentCloudHeight, 0.5), 1.1); // range from 0-1.5;

    return (diceFactor * heightFactor * floorFactor * extraFactor);
  }

  // extraFactor is used for growing clouds
  public static boolean isChosen(double cloudHeight, double floorHeight, double extraFactor, double threshold) {
    return rollDice(cloudHeight, floorHeight, extraFactor) > threshold;
  }

  // percentHeight is from 0-1 // for spawning new clouds
  public static boolean isChosen(double cloudHeight, double floorHeight, double threshold) {
    return isChosen(cloudHeight, floorHeight, 1, threshold);
  }

  public static Location RandomNear(Location loc, int radius) {
    // pick random location near player (x,z)
    Location groundLoc = loc.clone();
    int locX = ThreadLocalRandom.current().nextInt(0, radius) - radius / 2;
    int locZ = ThreadLocalRandom.current().nextInt(0, radius) - radius / 2;
    groundLoc.add(locX, 0, locZ);

    // move height to ground
    return RainUtil.getGroundAt(groundLoc);
  }

  public static Vector Random() {
    return new Vector(
        ThreadLocalRandom.current().nextDouble() * 2 - 1,
        ThreadLocalRandom.current().nextDouble() * 2 - 1,
        ThreadLocalRandom.current().nextDouble() * 2 - 1
    );
  }

  /**
   * Get the factor based on if its a corner, top, bottom, and/or sides
   * Factors multiply if intersecting
   * @return normalized double which can be applied to randomly choosing neighbors
   */
  public static Double getFactor(Map<String, Double> factors, int x, int y, int z) {
    double factor = 1.0;

    if (Math.abs(x) + Math.abs(z) == 2) { // corners
      factor *= factors.get("corners");
    }
    if (y == 1) { // top
      factor *= factors.get("top");
    } else if (y == -1) { // bottom
      factor *= factors.get("bottom");
    }
    if (Math.abs(x - z) == 1) { // sides
      factor *= factors.get("sides");
    }

    return factor;
  }


  public static Double cosineSimilarity(Vector a, Vector b) {
    return a.dot(b) / (a.length() * b.length());
  }

  public static Vector tanh(Vector vec) {
    return new Vector(
      Math.tanh(vec.getX()),
      Math.tanh(vec.getY()),
      Math.tanh(vec.getZ())
    );
  }

  public static Block[] neighbors(Block block) {
    return new Block[] {
      block.getRelative(1, 0, 0),
      block.getRelative(-1, 0, 0),
      block.getRelative(0, 1, 0),
      block.getRelative(0, -1, 0),
      block.getRelative(0, 0, 1),
      block.getRelative(0, 0, -1)
    };
  }

  public static Chunk[] neighbors(Chunk chunk) {
    return new Chunk[] {
      chunk.getWorld().getChunkAt(chunk.getX() + 1, chunk.getZ()),
      chunk.getWorld().getChunkAt(chunk.getX() - 1, chunk.getZ()),
      chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ() + 1),
      chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ() - 1)
    };
  }

  public static Set<Block> allBlocks() {
    Set<Chunk> chunks = new HashSet<Chunk>();
    for (World world : Bukkit.getWorlds()) {
      for (Chunk chunk : world.getLoadedChunks()) {
        chunks.add(chunk);
      }
    }
    return allBlocks(chunks);
  }

  public static Set<Block> allBlocks(Chunk chunk) {
    return allBlocks(new HashSet<Chunk>() {{ add(chunk); }});
  }

  public static Set<Block> allBlocks(Set<Chunk> chunks) {
    Set<Block> blocks = new HashSet<Block>();
    for (Chunk chunk : chunks) {
      int maxHeight = chunk.getWorld().getMaxHeight(); 
      for (int x = 0; x < 16; x++) {
        for (int z = 0; z < 16; z++) {
          for (int y = 0; y < maxHeight; y++) {
            blocks.add(chunk.getBlock(x, y, z)); 
          }
        }
      }
    }
    return blocks;
  }

  public static Set<Block> topAirs(Chunk playerChunk) {
    Set<Block> blocks = new HashSet<Block>();
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        blocks.add(topAir(playerChunk, x, z));
      }
    }
    return blocks;
  }

  public static Block topAir(Chunk chunk, int x, int z) {
    Location chunkLoc = chunk.getBlock(x, 0, z).getLocation();
    return chunk.getWorld().getHighestBlockAt(chunkLoc).getRelative(BlockFace.UP);
  }

  public static double poolTemperature(Double[] temperatures) {
    if (RainUtil.cachedPoolTemperatures.containsKey(temperatures)) {
      return RainUtil.cachedPoolTemperatures.get(temperatures);
    }

    // get avg temperature
    double totalTemperature = 0;
    for (double temperature : temperatures) {
      totalTemperature += temperature;
    }
    double averageTemperature = totalTemperature / 6.0;

    // cache for next time
    RainUtil.cachedPoolTemperatures.put(temperatures, averageTemperature);
    return averageTemperature;
  }

  public static double temperaturize(Block block) {
    if (block.hasMetadata("temperature")) {
      return block.getMetadata("temperature").get(0).asDouble();
    }
    if (!RainUtil.isAir(block)) {
      return 1;
    }
    return 0;
  }

  public static double humidify(Block block) {
    if (block.hasMetadata("humidity")) {
      return block.getMetadata("humidity").get(0).asDouble();
    }
    return 0;
  }

  public static double pressurize(Block block) {
    double temperature = RainUtil.temperaturize(block);
    // humidity lowers pressure
    double humidity = RainUtil.humidify(block);
    return temperature * (1 - humidity);
  }

  public static Double[] reversePoolTemperature(double temperature, Double[] temperatures) {
    // use a tuple/pair/entry pair as key
    if (RainUtil.cachedReversePoolTemperatures.containsKey(new AbstractMap.SimpleEntry<Double, Double[]>(temperature, temperatures))) {
      return RainUtil.cachedReversePoolTemperatures.get(new AbstractMap.SimpleEntry<Double, Double[]>(temperature, temperatures));
    }
    Double[] newTemperatures = new Double[temperatures.length];
    for (int i = 0; i < temperatures.length; i++) {
      // dont change solids temperature (optional)
      if (temperatures[i] == 0) {
        newTemperatures[i] = null;
        continue;
      }
      // remove average temperature
      newTemperatures[i] = temperatures[i] + (temperatures[i] + temperature) / 2;
    }

    // cache for next time
    RainUtil.cachedReversePoolTemperatures.put(new AbstractMap.SimpleEntry<Double, Double[]>(temperature, temperatures), newTemperatures);
    return newTemperatures;
  }

  public static int heatDirection(Block posAxis, Block negAxis) {
    double axisDelta = RainUtil.pressurize(posAxis) - RainUtil.pressurize(negAxis);
    return (int) Math.round(axisDelta);
  }

  public static Block[] windDirection(Block block, int count) {
    // follow the temperature gradient
    Block[] neighbors = RainUtil.neighbors(block);
    Vector velocity = new Vector(
      heatDirection(neighbors[0], neighbors[1]),
      heatDirection(neighbors[2], neighbors[3]),
      heatDirection(neighbors[4], neighbors[5])
    );
    // collect blocks
    Block[] nextBlocks = new Block[count];
    int idx = 0;
    while (idx < count) { 
      Vector delta = null;
      if (idx > 0) {
        // remove delta from axisVelocities
        delta = nextBlocks[idx-1].getLocation().toVector().subtract(block.getLocation().toVector());
        velocity = velocity.subtract(delta);
      }
      // get next block
      nextBlocks[idx] = block.getRelative(velocity.getBlockX(), velocity.getBlockY(), velocity.getBlockZ());
      idx++;
    }
    return nextBlocks;
  }
}
