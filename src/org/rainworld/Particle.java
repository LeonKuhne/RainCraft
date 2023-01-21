package org.rainworld;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.util.Vector;

public class Particle {
  private Double mass;
  private Vector position;
  private Vector velocity;

  public Particle(
    Double mass, Vector position, Vector velocity
  ) {
    this.mass = mass;
    this.position = position;
    this.velocity = velocity;
  }

  public List<Particle> decay(Double decayForce) {
    // split particle into two new particles
    // with half the mass and conserving velocities
    List<Particle> particles = new ArrayList<Particle>();
    for (int dir = -1; dir < 2; dir+=2) {
      particles.add(
        new Particle(
          mass / 2,
          position.clone(),
          velocity.clone().multiply(decayForce / mass * dir)
        )
      ); 
    } 
    return particles;
  }

  public void collapse(
    List<Particle> particles, Double collapseRadius
  ) {
    // attract particles if within range
    for (Particle particle : particles) {
      if (particle == this) {
        continue;
      }
      Vector delta = particle.position.clone().subtract(position);
      if (delta.length() < collapseRadius) {
        // attract particle
        Vector force = delta.clone().normalize().multiply(
          mass * particle.mass / delta.lengthSquared()
        );
        velocity.add(force);
        particle.velocity.subtract(force);
      }
    } 
  }
}