package org.rainworld.physics;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.rainworld.Rain;
import org.rainworld.RainUtil;
import org.bukkit.util.Vector;

public class ParticleEngine {
  public ParticleManager particles;
  private int ticks;
  private Set<Location> prevLocations;

  public ParticleEngine() {
    ticks = 0;
    particles = new ParticleManager();
    prevLocations = new HashSet<Location>();
  }

  public void tick() {
    if (ticks % Rain.config.ticksPerDecay == 0) decay();
    if (ticks % Rain.config.ticksPerMove == 0) move();
    if (ticks % Rain.config.ticksPerAttract == 0) attract();
    if (ticks % Rain.config.ticksPerInteract == 0) interact();
    if (ticks % Rain.config.ticksPerDraw == 0) draw();
    ticks++;
  }

  public void addParticles(Vector pos, int numParticles) {
    addParticles(pos, numParticles, Rain.config.particleSpawnMass);
  }
  public void addParticles(Vector pos, int numParticles, double mass) {
    Rain.log(String.format("spawning: %d", numParticles));
    Vector direction = RainUtil.Random().multiply(Rain.config.cloudSpeed);
    for (int i = 0; i < numParticles; i++) {
      Vector offsetDirection = RainUtil.Random().multiply(Rain.config.attractForce);
      particles.add(new Particle(mass, pos.clone(), direction.add(offsetDirection)));
    }
  }

  private void move() { particles.move(); }

  private void decay() {
    // OLD METHOD 
    // split particles into opposite and random directions
    //Rain.log(String.format("decaying: %d -> %d", particles.size(), 2 * particles.size()));
    //particles.remap(particle -> particle.split(Rain.config.decayForce, RainUtil.Random()));
  }

  // EXTRA: attract/repel similar/dissimilar temperatures
  private void attract() { particles.attract(); }

  // combine particles
  private void interact() {
    if (particles.size() == 0) return;

    Set<Particle> toDelete = new HashSet<Particle>();
    Set<Particle> toAdd = new HashSet<Particle>();

    // combine similar particles
    BiConsumer<Particle, Particle> tryCombine = (p1, p2) -> {
      if (RainUtil.cosineSimilarity(p1.velocity, p2.velocity) > Rain.config.combineThreshold) {
        Particle newParticle = combineParticles(p1, p2);
        // reconcile
        toAdd.add(newParticle);
        toDelete.add(p1);
        toDelete.add(p2);
      }
    };

    // apply friction
    BiConsumer<Particle, Particle> applyFriction = (p1, p2) -> {
      Vector friction = p1.velocity.clone().subtract(p2.velocity).multiply(Rain.config.frictionForce);
      Double weight = p2.mass / p1.mass;
      Rain.log(String.format("friction: %s, weight: %s", friction, weight));
      p1.accel(friction, weight);
      //p2.accel(friction.multiply(-1), 1 / weight);
    };

    // match nearby particles
    for (Set<Particle> group : particles.groups.values()) {
      for (Particle source : group) { 
        if (toDelete.contains(source)) continue;
        for (Particle target : particles.nearby(source).keySet()) {
          if (toDelete.contains(target)) continue;
          //tryCombine.accept(source, target);
          applyFriction.accept(source, target);
        }
      }
    }

    // update particles
    //Rain.log(String.format("collapsing: %d -> %d", particles.size(), particles.size() - toDelete.size() + toAdd.size()));
    particles.removeAll(toDelete);
    particles.addAll(toAdd);
  }

  private Particle combineParticles(Particle p1, Particle p2) {
    //Rain.log(String.format("combining: %s, %s", p1, p2));
    p1.mass += p2.mass;
    // m1v2 + m2v2 = (m1 + m2)vT
    // vT = (m1v1 + m2v2) / (m1 + m2)
    Vector momentum1 = p1.velocity.multiply(p1.mass);
    Vector momentum2 = p2.velocity.multiply(p2.mass);
    Vector totalMomentum = momentum1.add(momentum2);
    double totalMass = p1.mass + p2.mass;
    Vector newVelocity = totalMomentum.multiply(1 / totalMass);
    // update particles
    p1.velocity = newVelocity;
    return p1;
  }

  private void draw() {
    Set<Location> newLocations = new HashSet<>(
      particles.map(particle -> particle.position.toLocation(RainUtil.getOverworld()))
    );
    
    // to delete = those that are in prev but not in new
    Set<Location> toDelete = new HashSet<Location>(prevLocations);
    toDelete.removeAll(newLocations);
    toDelete.forEach(loc -> RainUtil.fill(loc, Material.AIR));

    // to add = those that are in new but not in prev
    Set<Location> toAdd = new HashSet<Location>(newLocations);
    toAdd.removeAll(prevLocations);
    toAdd.forEach(loc -> RainUtil.fill(loc, Material.WHITE_WOOL));

    // update prevLocations
    prevLocations = newLocations;

    // debug
    Rain.log(String.format("drawing: %d, -%d +%d", particles.size(), toDelete.size(), toAdd.size()));
  }
}
