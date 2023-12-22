package org.rainworld.physics;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.rainworld.Rain;
import org.rainworld.RainUtil;

public class Particle {
  private static int nextId = 0;

  public int id;
  public Double mass;
  public Vector position;
  public Vector velocity;
  private boolean debug = true;

  public Particle(
    Double mass, Vector position, Vector velocity
  ) {
    this.id = Particle.nextId++;
    this.mass = mass;
    this.position = position;
    this.velocity = velocity;
  }

  public void move() { move(velocity); }
  public void move(Vector velocity) {
    position.add(velocity);
  }

  // intercepts: (0 -1), (1 0), in range [0, 1] -> out range [-1, 1]
  public void attract(Particle other, Double normDistance) {
    //Function<Double, Double> curve = (x) -> Math.sin(x * 2 * Math.PI) * Math.pow(x, 2);         // https://www.desmos.com/calculator/vchon47wlj
    Function<Double, Double> curve = (x) -> { // https://www.desmos.com/calculator/4z9yncbhcy 
      Double h = Math.sqrt(x);
      return -Math.sin(h * 2 * Math.PI) * h;
    };         
    //Function<Double, Double> curve = (x) -> -Math.pow(-(x-1), 3) - Math.pow(2*(x-1), 7);
    Double force = curve.apply(normDistance) * Rain.config.attractForce;
    Vector delta = other.position.clone().subtract(this.position);
    // randomize delta if zero
    Vector momentum;
    Rain.log(String.format("distance: %f", normDistance));
    if (normDistance == 0) { 
      momentum = RainUtil.Random()
        .multiply(2)
        .subtract(new Vector(1, 1, 1))
        .multiply(Rain.config.jitterDistance); 
    } else { 
      momentum = delta.clone().multiply(force / normDistance); 
    }
    Double weight = other.mass / (this.mass + other.mass);
    // update velocities
    accel(momentum, weight);
    //other.accelerate(momentum.multiply(-1), 1 - weight);
  }

  public void accel(Vector momentum, Double weight) {
    velocity.add(momentum.multiply(weight));
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

  public int x() { return (int) position.getBlockX(); }
  public int y() { return (int) position.getBlockY(); }
  public int z() { return (int) position.getBlockZ(); }
  public Block block() {
    return RainUtil.getOverworld().getBlockAt(x(), y(), z());
    // return position.toLocation(RainUtil.getOverworld()).getBlock();
  }

  public boolean equals(Object other) {
    if (other instanceof Particle) {
      Particle otherParticle = (Particle) other;
      return this.id == otherParticle.id;
    }
    return false;
  }
}