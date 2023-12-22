package org.rainworld;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;

public class Config {
  // timings
  public int ticksPerSpawn = 6;
  public int ticksPerDecay = 1;
  public int ticksPerInteract = 1;
  public int ticksPerMove = 1;
  public int ticksPerDraw = 1;
  public int ticksPerAttract = 1;
  // physics
  public double maxVelocity = 1.0;
  public double collapseRadius = 0.1;
  // spawning
  public int particlesPerSpawn = 20;
  public double particleSpawnMass = 1;
  public double combineThreshold = 0.99;
  public double attractDistance = 10; // measured in blocks
  public double attractForce = 0.1;
  public double cloudSpeed = 0.001;
  public double frictionForce = 0.001;
  public double jitterDistance = 0.01;
  // debug
  public int duplicateHistory = 5;
  // legacy
  public int cloudHeight = 20;
  public int cloudVariance = 10;
  public int cloudSpawnWidth = 50;
  public int maxClouds = 1000;
  public int raindropDelay = 50;
  public double cloudSpawnThreshold = 0.95;
  // NEW
  public int colorScale = 3; 
  public double minTemperature = 1 / Math.pow(6, 27); // 6 sides, n blocks above from peak
  public List<Entry<String, Double>> colors = new ArrayList<>(); 
  {
    // map blue->red stained glass materials to temperature thresholds
    colors.add(new HashMap.SimpleEntry<String, Double>("BLUE_STAINED_GLASS", colorize(8)));
    colors.add(new HashMap.SimpleEntry<String, Double>("LIGHT_BLUE_STAINED_GLASS", colorize(7)));
    colors.add(new HashMap.SimpleEntry<String, Double>("CYAN_STAINED_GLASS", colorize(6)));
    colors.add(new HashMap.SimpleEntry<String, Double>("GREEN_STAINED_GLASS", colorize(5)));
    colors.add(new HashMap.SimpleEntry<String, Double>("LIME_STAINED_GLASS", colorize(4)));
    colors.add(new HashMap.SimpleEntry<String, Double>("YELLOW_STAINED_GLASS", colorize(3)));
    colors.add(new HashMap.SimpleEntry<String, Double>("ORANGE_STAINED_GLASS", colorize(2)));
    colors.add(new HashMap.SimpleEntry<String, Double>("RED_STAINED_GLASS", colorize(1)));
    colors.add(new HashMap.SimpleEntry<String, Double>("MAGENTA_STAINED_GLASS", colorize(0)));
  }

  private double colorize(int distance) {
    return 1 / Math.pow(6, distance * colorScale);
  }

  public boolean has(String key) {
    try {
      this.getClass().getField(key);
      return true;
    } catch (NoSuchFieldException | SecurityException e) {
      return false;
    }
  }

  public void set(String key, String value) {
    try {
      Field field = this.getClass().getField(key);
      // parse to correct type
      if (field.getType() == int.class) {
        field.set(this, Integer.parseInt(value));
      } else if (field.getType() == double.class) {
        field.set(this, Double.parseDouble(value));
      } else if (field.getType() == boolean.class) {
        field.set(this, Boolean.parseBoolean(value));
      } else {
        field.set(this, value);
      }
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    StringBuilder help = new StringBuilder();
    help.append(ChatColor.BLUE + "RainWorld Config\n");
    for (Field field : this.getClass().getFields()) {
      try {
        // add field name and value to help with chat color
        help.append(ChatColor.GRAY + field.getName() + ChatColor.GOLD + ": " + field.get(this) + "\n");
      } catch (IllegalArgumentException | IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return help.toString();
  }
}