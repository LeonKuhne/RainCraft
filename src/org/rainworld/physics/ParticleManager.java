package org.rainworld.physics;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.util.Consumer;

import weka.core.Attribute;
import weka.core.Instances;

public class ParticleManager extends Instances {

  private List<Particle> particles;

  public ParticleManager(ArrayList<Attribute> attributes) {
    super(new Instances("Particles", attributes, 0));
    particles = new ArrayList<Particle>();
  }

  public void add(Particle particle) {
    super.add(particle);
    particles.add(particle);
  }

  public void addAll(List<Particle> particles) {
    super.addAll(particles);
    particles.addAll(particles);
  }

  public void del(Particle particle) {
    super.remove(particle);
    particles.remove(particle);
  }

  public void clear() {
    super.clear();
    particles.clear();
  }

  public void each(Consumer<Particle> consumer) {
    for (Particle particle : particles) {
      consumer.accept(particle);
    }
  }

  public void map(Function<Particle, List<Particle>> update) {
    ArrayList<Particle> newParticles = new ArrayList<Particle>();
    for (Particle particle : particles) {
      newParticles.addAll(update.apply(particle));
    }
    clear();
    addAll(newParticles);
  }
}
