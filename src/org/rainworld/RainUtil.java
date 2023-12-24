package org.rainworld;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    int locX = Rain.rng.nextInt(radius) - radius / 2;
    int locZ = Rain.rng.nextInt(radius) - radius / 2;
    groundLoc.add(locX, 0, locZ);

    // move height to ground
    return RainUtil.getGroundAt(groundLoc);
  }

  public static Vector Random() {
    Supplier<Double> rand = () -> Rain.rng.nextDouble() * 2 - 1;
    return new Vector(rand.get(), rand.get(), rand.get());
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

  public static double average(double[] values) {
    double sum = 0;
    for (double val : values) { sum += val; }
    return sum / values.length;
  }

  public static double[] reversePoolAverage(double[] params) {
    double[] neighbors = new double[params.length - 1];
    double center = params[0];
    for (int i = 0; i < neighbors.length; i++) {
      neighbors[i] += (center - params[i+1]) * Rain.config.spreadFactor; 
    }
    return neighbors;
  }
}
