package org.rainworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class RainUtil {
  static final int CLOUD_HEIGHT = 20;
  static final int CLOUD_VARIANCE = 10;
  static final int CLOUD_SPAWN_WIDTH = 50;
  static final int MAX_CLOUDS = 1000;
  static final int RAINDROP_DELAY = 50;
  static BukkitScheduler scheduler = null;
  static Plugin plugin = null;
  static Map<Integer, Integer> sessions = new HashMap<Integer, Integer>();

  // THREADING
  //

  public static BukkitScheduler scheduler(Plugin plugin) {
    RainUtil.plugin = plugin;
    scheduler = plugin.getServer().getScheduler();
    return scheduler;
  }

  private static void decrement(int sessionId) {
    sessions.put(sessionId, sessions.get(sessionId) - 1);
  }

  public static BukkitTask async(Runnable task) {
    return scheduler.runTaskAsynchronously(plugin, task);
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
    int height = CLOUD_HEIGHT + (int) Math.round(Math.random() * CLOUD_VARIANCE);
    cloudLoc.add(0, height, 0);
    return cloudLoc;
  }

  // RANDOM
  //

  public static void debug(Location loc) {
    loc.getBlock().setType(Material.REDSTONE_BLOCK);
  }

  public static double rollDice(double cloudHeight, double floorHeight, double extraFactor) {
    double percentWorldHeight = cloudHeight / (getOverworld().getMaxHeight());
    double percentCloudHeight = Math.abs(floorHeight / CLOUD_HEIGHT);

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
        ThreadLocalRandom.current().nextInt(-1, 2),
        ThreadLocalRandom.current().nextInt(-1, 2),
        ThreadLocalRandom.current().nextInt(-1, 2)
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
}
