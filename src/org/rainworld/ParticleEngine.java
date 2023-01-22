package org.rainworld;

import java.util.ArrayList;
import java.util.List;
//import weka.clusters.SimpleKMeans;

public class ParticleEngine {
  public static class Config {
    public Double decayForce = 0.5;
    public Double collapseRadius = 0.1;
    public int ticksPerDecay = 1;
    public int ticksPerCollapse = 1;
    public int ticksPerMove = 3;
    public int ticksPerDraw = 1;
  }

  Config config;
  private List<Particle> particles;
  private int ticks;  

  public ParticleEngine(Config config) {
    particles = new ArrayList<Particle>();
    this.config = config;
    ticks = 0;
  }

  public void tick() {
    // NOTE reordering these steps could be interesting
    if (ticks % config.ticksPerDecay == 0) decay();
    if (ticks % config.ticksPerCollapse == 0) collapse();
    if (ticks % config.ticksPerMove == 0) move();
    if (ticks % config.ticksPerDraw == 0) draw();
    ticks++;
  }

  // combine particles
  private void collapse() {
    // find kmeans clusters
    // combine tightest clusters
    // update particles
  }

  private void move() {
    // with all of the particles within surrounding chunks
    // atract based on distance

    // EXTRA
    // attract those that have similar temperatures
    // repel those that have different temperatures 
  }

  private void decay() {
    List<Particle> newParticles = new ArrayList<Particle>();
    particles.forEach(particle -> {
      newParticles.addAll(
        particle.decay(config.decayForce));
    });
    particles = newParticles;
  }

  private void draw() {
    particles.forEach(particle -> {
      // turn particle into block location
      // mark new block location if valid
      // remove previous locations if not in new list
      // render blocks at new locations
    });
  }
}
