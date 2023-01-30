package org.rainworld;

import java.lang.reflect.Field;

import org.bukkit.ChatColor;

public class Config {
  // timings
  public int ticksPerSpawn = 15;
  public int ticksPerDecay = 3;
  public int ticksPerCollapse = 15;
  public int ticksPerMove = 3;
  public int ticksPerDraw = 1;
  public int ticksPerAttract = 3;
  // physics
  public Double decayForce = 1.0;
  public Double collapseRadius = 0.1;
  // spawning
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
      field.set(this, value);
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