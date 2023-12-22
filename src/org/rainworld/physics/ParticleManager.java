package org.rainworld.physics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.bukkit.block.Block;
import org.bukkit.util.Consumer;

public class ParticleManager {

  public HashMap<Block, Set<Particle>> groups;

  public ParticleManager() {
    groups = new HashMap<Block, Set<Particle>>();
  }

  public void add(Particle particle) {
    Block block = particle.block();
    groups.putIfAbsent(block, new HashSet<Particle>());
    groups.get(block).add(particle);
  }

  public void addAll(Set<Particle> particles) {
    for (Particle particle : particles) {
      add(particle);
    }
  }

  public void remove(Particle particle) {
    Block block = particle.block();
    groups.get(block).remove(particle);
    if (groups.get(block).size() == 0) {
      groups.remove(block);
    }
  }

  public void removeAll(Set<Particle> particles) {
    for (Particle particle : particles) {
      remove(particle);
    }
  }

  public void clear() {
    groups.clear();
  }

  public void each(BiConsumer<Particle, Consumer<Runnable>> consumer) {
    Set<Runnable> postActions = new HashSet<Runnable>();
    each(particle -> consumer.accept(particle, (postAction) -> postActions.add(postAction)));
    for (Runnable action : postActions) action.run();
  }
  public void each(Consumer<Particle> consumer) {
    for (Set<Particle> particles : groups.values()) {
      for (Particle particle : particles) {
        consumer.accept(particle);
      }
    }
  }

  public <T> Set<T> map(Function<Particle, T> update) {
    Set<T> newParticles = new HashSet<T>();
    each(particle -> newParticles.add(update.apply(particle)));
    return newParticles;
  }

  public void remap(Function<Particle, Set<Particle>> update) {
    Set<Particle> newParticles = new HashSet<Particle>();
    each(particle -> newParticles.addAll(update.apply(particle)));  
    clear();
    addAll(newParticles);
  }

  public void move() {
    each((particle, after) -> {
      Block prevBlock = particle.block();
      particle.move();
      Block newBlock = particle.block();
      if (prevBlock.equals(newBlock)) return;
      // update groups
      after.accept(() -> {
        switchGroup(prevBlock, newBlock, particle);
      });
    });
  }

  public void attract() {
    each(source -> {
      for (Entry<Particle, Double> entry : nearby(source, true).entrySet()) {
        // attract based on distance
        Particle target = entry.getKey();
        double normDistance = entry.getValue() / target.mass;
        source.attract(target, normDistance);
      }
    });
  }

  public int size() {
    int size = 0;
    for (Set<Particle> particles : groups.values()) {
      size += particles.size();
    }
    return size;
  }

  public Map<Particle, Double> nearby(Particle particle) { 
    return nearby(particle, 1); 
  }
  public Map<Particle, Double> nearby(Particle particle, boolean useMassAsRange) {
    return nearby(particle, particle.mass);
  } 
  public Map<Particle, Double> nearby(Particle source, double maxDistance) {
    Map<Particle, Double> neighbors = new HashMap<Particle, Double>();
    Block sourceBlock = source.block();
    int radius = (int) Math.ceil(maxDistance);
    for (int x = -radius; x <= radius; x++) {
      for (int y = -radius; y <= radius; y++) {
        for (int z = -radius; z <= radius; z++) {
          Block targetBlock = sourceBlock.getRelative(x, y, z);
          if (!groups.containsKey(targetBlock)) continue;
          for (Particle target : groups.get(targetBlock)) {
            if (target.equals(source)) continue;
            double distance = source.position.distance(target.position);
            if (distance > maxDistance) continue;
            neighbors.put(target, distance);
          }
        }
      }
    }
    return neighbors;
  }

  public void switchGroup(Block prevBlock, Block newBlock, Particle particle) {
    groups.get(prevBlock).remove(particle);
    groups.putIfAbsent(newBlock, new HashSet<Particle>());
    groups.get(newBlock).add(particle);
  }
}