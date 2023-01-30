package org.rainworld.physics;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.util.Vector;
import weka.core.DenseInstance;

public class Particle extends DenseInstance {
  public Double mass;
  public Vector position;
  public Vector velocity;

  public Particle(
    Double mass, Vector position, Vector velocity
  ) {
    super(4); // x, y, z, temperature
    this.mass = mass;
    this.position = position;
    this.velocity = velocity;
  }

  public void move() {
    position.add(velocity);
  }

  // split particle in two with half mass conserving momentum
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