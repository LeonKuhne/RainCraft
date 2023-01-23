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

  public void move() {
    position.add(velocity);
  }

  // split particle with half mass conserving momentum
  public List<Particle> split(Double force, Vector direction) {
    List<Particle> particles = new ArrayList<Particle>();
    for (int dir = -1; dir < 2; dir+=2) {
      Vector velocity = direction.multiply(force / mass * dir);
      velocity.add(this.velocity);
      particles.add(
        new Particle(mass / 2, position.clone(), velocity)
      ); 
    } 
    return particles;
  }

  public String toString() {
    return String.format(
      "Particle(mass=%s, position=%s, velocity=%s)",
      mass, position, velocity
    );
  }
}