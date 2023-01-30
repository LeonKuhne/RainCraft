package org.rainworld.physics;
import java.util.ArrayList;
import org.bukkit.Location;
import org.rainworld.Rain;
import org.rainworld.RainUtil;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;

public class ParticleEngine {
  public ParticleManager particles;
  private int ticks;

  public ParticleEngine() {
    ticks = 0;
    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    attributes.add(new Attribute("x"));
    attributes.add(new Attribute("y"));
    attributes.add(new Attribute("z"));
    attributes.add(new Attribute("temperature"));
    particles = new ParticleManager(attributes);
  }

  public void tick() {
    // NOTE reordering these steps could be interesting
    if (ticks % Rain.config.ticksPerDecay == 0) decay();
    if (ticks % Rain.config.ticksPerMove == 0) move();
    if (ticks % Rain.config.ticksPerAttract == 0) attract();
    if (ticks % Rain.config.ticksPerCollapse == 0) collapse();
    if (ticks % Rain.config.ticksPerDraw == 0) draw();
    ticks++;
  }

  // split particles into opposite and random directions
  private void decay() {
    Rain.log(String.format("decaying: %d -> %d", particles.size(), 2 * particles.size()));
    particles.map(particle -> particle.split(Rain.config.decayForce, RainUtil.Random()));
  }

  private void move() {
    Rain.log(String.format("moving: %d", particles.size()));
    particles.each(particle -> particle.move());
  }

  // EXTRA: attract/repel similar/dissimilar temperatures
  private void attract() {
    Rain.log(String.format("attracting: %d", particles.size()));
    // find all particles in this and surrounding chunks
    // attract based on distance
  }

  // combine particles
  private void collapse() {
    int numClusters = 5;
    Rain.log(String.format("collapsing: %d -> %d", particles.size(), numClusters));
    // find kmeans clusters
    SimpleKMeans kmeans = new SimpleKMeans();
    try {
      kmeans.setNumClusters(numClusters);
      kmeans.buildClusterer(particles);
    } catch (Exception e) {
        e.printStackTrace();
    }

    // combine tightest clusters
    // update particles
  }

  private void draw() {
    Rain.log(String.format("drawing: %d", particles.size()));
    particles.each(particle -> {
      // mark new block location (if valid)
      Location loc = particle.position.toLocation(RainUtil.getOverworld());
      RainUtil.debug(RainUtil.cloudAbove(loc));
      Rain.log(String.format("pos: %s", loc));
      // remove previous locations if not in new list
      // render blocks at new locations
    });
  }
}
