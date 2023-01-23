package org.rainworld;

import java.util.ArrayList;
import java.util.List;
//import weka.clusters.SimpleKMeans;

public class ParticleEngine {
  public static class Config {
    public Double decayForce = 0.5;
    public Double collapseRadius = 0.1;
    // NOTE adjusting these timings could be interesting
    public int ticksPerDecay = 1;
    public int ticksPerCollapse = 1;
    public int ticksPerMove = 3;
    public int ticksPerDraw = 1;
    public int ticksPerAttract = 3;
  }

  Config config;
  private TODO<Particle> particles;
  private int ticks;

  public ParticleEngine(Config config) {
    particles = new ArrayList<Particle>();
    this.config = config;
    ticks = 0;
  }

  public void tick() {
    // NOTE reordering these steps could be interesting
    if (ticks % config.ticksPerDecay == 0) decay();
    if (ticks % config.ticksPerMove == 0) move();
    if (ticks % config.ticksPerAttract == 0) attract();
    if (ticks % config.ticksPerCollapse == 0) collapse();
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
    // apply velocities
  }

  // EXTRA: attract/repel similar/dissimilar temperatures
  private void attract() {
    // find all particles in this and surrounding chunks
    // attract based on distance
  }

  // split particles into opposite and random directions
  private void decay() {
    List<Particle> newParticles = new ArrayList<Particle>();
    particles.forEach(particle -> {
      newParticles.addAll(
        particle.split(config.decayForce, RainUtil.Random()));
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
