package org.rainworld;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;

public class Gradient {
  
  private String metadataKey;
  private double defaultValue;
  private List<Function<Block, Double>> defaultFilters;
  private Cache<double[], Double> poolAverageCache;
  private Cache<double[], double[]> reversePoolCache;

  public Gradient(String metadataKey) { this(metadataKey, 0); }
  public Gradient(String metadataKey, double defaultValue) {
    this.metadataKey = metadataKey;
    this.defaultValue = defaultValue;
    this.defaultFilters = new ArrayList<>();
    this.poolAverageCache = new Cache<double[], Double>();
    this.reversePoolCache = new Cache<double[], double[]>();
  }

  public void addDefault(Function<Block, Double> filter) {
    defaultFilters.add(filter);
  }

  // 
  // ACCESSORS

  public boolean contains(Block block) {
    return block.hasMetadata(metadataKey);
  }

  public double read(Block block) {
    // metadata
    if (this.contains(block)) { 
      return block.getMetadata(metadataKey).get(0).asDouble();
    }
    // filters
    if (defaultFilters != null) {
      for (Function<Block, Double> filter : defaultFilters) {
        Double value = filter.apply(block); 
        if (value != null) { return value; }
      }
    }
    // default
    return defaultValue; 
  }

  public void update(Block block, double value) {
    block.setMetadata(metadataKey, new FixedMetadataValue(Rain.plugin, value));
  }

  public void reset(Block block) {
    block.removeMetadata(this.metadataKey, Rain.plugin);
  }

  public void combine(Block block, double humidity) {
    this.update(block, humidity + this.read(block));
  }

  // 
  // POOLING

  public double poolAverage(double[] neighbors) {
    return poolAverageCache.load(RainUtil::average, neighbors);
  }

  public double[] reversePoolAverage(Double center, double[] neighbors) {
    double[] params = new double[neighbors.length + 1];
    params[0] = center;
    for (int i = 0; i < neighbors.length; i++) params[i+1] = neighbors[i];
    return reversePoolCache.load(RainUtil::reversePoolAverage, params);
  }

  public double[] neighbors(Block block) {
    Block[] neighbors = RainUtil.neighbors(block);
    double[] values = new double[neighbors.length];
    for (int i = 0; i < neighbors.length; i++) {
      values[i] = this.read(neighbors[i]);
    }
    return values;
  }
}
