package org.rainworld;

import java.lang.reflect.Field;

import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

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