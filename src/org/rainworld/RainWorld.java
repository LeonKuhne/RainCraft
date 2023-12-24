package org.rainworld;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.rainworld.command.RainCommand;

public class RainWorld extends JavaPlugin {

  private SimpleCommandMap commands;
  private Set<Chunk> loadedChunks;
  private TrackedGradient temperatures;
  private TrackedGradient humidities;

  @Override
  public void onEnable() {
    Rain.init(this);
    
    temperatures = new TrackedGradient("temperature");
    temperatures.addDefault(block -> !RainUtil.isAir(block) ? 1. : null);
    //temperatures.untrackWhen(block -> true); // only track once
    humidities = new TrackedGradient("humidity");
    humidities.trackWhen(block -> humidities.read(block) > Rain.config.minHumidity);

    //
    // COMMANDS 
    commands = new SimpleCommandMap(Rain.server);
    commands.registerAll("rain", Arrays.asList(
      RainCommand.create("cloud").player(info -> placeCloudAbove(info.player, null)),
      RainCommand.create("stop").anyone(info -> { destroyClouds(); }),
      RainCommand.create("watch").player(info -> Rain.listen(info.player)),
      RainCommand.create("ignore").player(info -> Rain.ignore(info.player)),
      RainCommand.create("config").player(info -> Rain.configure(info.sender, info.args))
        .setUsage("/rain config [<key> <value>]")
    ));

    //
    // EVENTS
    Rain.log("The rain is starting...");

    // strike chance if player under cloud
    Rain.server.getPluginManager().registerEvents(new PlayerUnderCloud(), this);

    // start processes
    //Rain.start(() -> generateClouds(), 5l)

    // load temperatures in players chunk
    loadedChunks = new HashSet<Chunk>();
    Rain.server.getPluginManager().registerEvents(new Listener() {
      @EventHandler
      public void onPlayerMove(PlayerMoveEvent event) {
        Chunk chunk = event.getTo().getChunk();
        if (event.getFrom().getChunk() == chunk) { return; }
        if (loadedChunks.contains(chunk)) { return; }
        Rain.log(String.format("Loading temperatures for chunk %s", chunk));
        loadTemperatures(chunk); 
        loadedChunks.add(chunk);
      }
    }, this);

    // spawn humidity above ground
    /*
    Rain.start(() -> {
      for (Player player : Rain.server.getOnlinePlayers()) {
        // pick random position in chunk
        Chunk chunk = player.getLocation().getChunk();
        Block air = RainUtil
          .topAir(chunk, Rain.rng.nextInt(16), Rain.rng.nextInt(16))
          .getRelative(BlockFace.UP);
        // spawn humidity
        humidities.combine(air, Rain.config.spawnHumidity);
      } 
    }, 5l);
    */

    // move humidity through the air
    // -> consider activating neighbors that have humidity by tracking them (or by updating their humidity)
    //Rain.start(() -> humidities.each(block -> moveHumidity(block)), 1l);
  }

  void moveHumidity(Block block) {
    if (!humidities.contains(block)) { throw new RuntimeException("humid has no humidity"); }
    double humidity = humidities.read(block);
    // spread humidity
    int spread = 1;
    if (humidity > Rain.config.humiditySpreadThreshold) { spread = 2; }
    // move humidity
    humidities.reset(block);
    for (Block nextBlock : windDirection(block, spread)) {
      if (!RainUtil.isAir(nextBlock)) { throw new RuntimeException("humidity moved the wrong way; into ground"); }
      humidities.combine(nextBlock, humidity);
    }
  }

  // create a blue->red dome around the player to visualize temperature using stained glass
  private void loadTemperatures(Chunk chunk) {
    Rain.log("assigning temperatures...");
    Set<Block> filled = spreadTemperatures(chunk);

    // render temperatures 
    for (Block block : filled) {
      double temperature = temperatures.read(block);
      if (Rain.config.showHeatMap) {
        drawTemperature(block, temperature);
      }
    };
  }

  // assumes colors are sorted smallest threshold to largest
  private void drawTemperature(Block block, Double temperature) {
    for (Map.Entry<String, Double> entry : Rain.config.colors) {
      if (temperature < entry.getValue()) { 
        block.setType(Material.getMaterial(entry.getKey()));
        return;
      }
    }
  }

  @Override
  public void onDisable() {
    Rain.stop();
    destroyClouds();
  }
  @Override
  public boolean onCommand(
    CommandSender sender, Command command, String label, String[] args
  ) {
    // execute '/rain' command
    if (label.equalsIgnoreCase("rain")) {
      String commandLine = String.join(" ", args);
      return commands.dispatch(sender, commandLine);
    }
    return false;
  }

  private void destroyClouds() {
    // TODO remove all clouds
  }

  private void placeCloudAbove(Player player, Map<String, Double> cloudFactors) {
    Location playerLocation = player.getLocation();
    Vector pos = RainUtil.cloudAbove(playerLocation).toVector();
    player.sendMessage(String.format("Placing a cloud above %s at %s", player.getName(), pos)); 
    // TODO generate cloud at pos
  }

  // pool average temperature into blocks metadata and recurse through neighbors
  // assumes temperatures includes blocks to start
  private Set<Block> spreadTemperatures(Chunk chunk) { 
    Set<Block> filled = new HashSet<Block>();

    // start on ground
    temperatures.track(RainUtil.topBlocks(chunk));

    // recurse through air
    while (!temperatures.isEmpty()) {
      Rain.log(String.format("processing: %d", temperatures.size()));
      // collect nearby
      Set<Block> neighbors = new HashSet<Block>();
      temperatures.each(source -> {
        for (Block block : RainUtil.neighbors(source)) {
          neighbors.add(block);
        }
      // before releasing changes
      // -> consider using parallel streams
      }, () -> {
        Set<Block> updated = neighbors
        .stream()
          .filter(block -> RainUtil.isAir(block))          // air
          //.filter(block -> !temperatures.contains(block))  // not already evaluated (only processes recently if untracking set to true, could be used for deeper recursion if these are allowed even if filled already, aka use 'tracked or not filled')
          .filter(block -> !filled.contains(block))      // (alt) not already evaluated
          .filter(block -> block.getChunk().equals(chunk)) // in same chunk
          .filter(block -> {
            // pool temperature
            double averageTemperature = temperatures.poolAverage(temperatures.neighbors(block));
            // stop if too cold
            if (averageTemperature < Rain.config.minTemperature) return false;
            // assign temperature
            temperatures.update(block, averageTemperature);
            return true;
          })
        .collect(Collectors.toSet());

        // use a stream to process changes
        filled.addAll(updated);
      });
    }

    return filled;
  }

  // follow the temperature gradient
  public Set<Block> windDirection(Block block, int count) {
    Block[] neighbors = RainUtil.neighbors(block);
    Vector velocity = new Vector(
      heatDirection(neighbors[0], neighbors[1]),
      heatDirection(neighbors[2], neighbors[3]),
      heatDirection(neighbors[4], neighbors[5])
    );
    Set<Block> nextBlocks = new HashSet<Block>();
    collectNextBlocks(block, velocity, next -> {
      nextBlocks.add(next);
      return nextBlocks.size() >= count;
    });
    return nextBlocks;
  }

  private void collectNextBlocks(Block block, Vector velocity, Function<Block, Boolean> collectBlock) {
    if (velocity.length() == 0) return;
    // find next block
    Block next = block.getRelative(velocity.getBlockX(), velocity.getBlockY(), velocity.getBlockZ());
    // stop if done
    if(collectBlock.apply(next)) return;
    // remove axis from velocity
    List<Supplier<Vector>> resetAxis = new ArrayList<Supplier<Vector>>(); 
    if (velocity.getBlockX() != 0) { resetAxis.add(() -> velocity.setX(0)); } 
    if (velocity.getBlockY() != 0) { resetAxis.add(() -> velocity.setY(0)); }
    if (velocity.getBlockZ() != 0) { resetAxis.add(() -> velocity.setZ(0)); }
    Vector nextVelocity = resetAxis.get(Rain.rng.nextInt(resetAxis.size())).get();
    // recurse
    collectNextBlocks(next, nextVelocity, collectBlock);
  }

  public double pressurize(Block block) {
    double temperature = this.temperatures.read(block);
    double humidity = this.humidities.read(block);
    // humidity lowers pressure
    return temperature * (1 - humidity);
  }

  public int heatDirection(Block posAxis, Block negAxis) {
    double axisDelta = this.pressurize(posAxis) - this.pressurize(negAxis);
    return (int) Math.round(axisDelta);
  }
}
