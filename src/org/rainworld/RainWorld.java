package org.rainworld;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.rainworld.command.RainCommand;

public class RainWorld extends JavaPlugin {

  private SimpleCommandMap commands;
  private Set<Chunk> loadedChunks;

  @Override
  public void onEnable() {
    Rain.init(this);

    //
    // COMMANDS 
    commands = new SimpleCommandMap(Rain.server);
    commands.registerAll("rain", Arrays.asList(
      RainCommand.create("cloud").player(info -> placeCloudAbove(info.player, null)),
      RainCommand.create("stop").anyone(info -> { destroyClouds(); }),
      RainCommand.create("watch").player(info -> Rain.listen(info.player)),
      RainCommand.create("ignore").player(info -> Rain.ignore(info.player)),
      RainCommand.create( "heat").player(info -> { loadTemperatures(info.player); }),
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

    // listen for new chunk loads
    loadedChunks = new HashSet<Chunk>();
    Rain.server.getPluginManager().registerEvents(new Listener() {
      // on player move events, check if chunk is loaded
      @EventHandler
      public void onPlayerMove(PlayerMoveEvent event) {
        Chunk chunk = event.getTo().getChunk();
        if (event.getFrom().getChunk() == chunk) { return; }
        if (loadedChunks.contains(chunk)) { return; }
        Rain.log(String.format("Loading temperatures for chunk %s", chunk));
        Player player = event.getPlayer();
        // load temperatures for chunk
        loadTemperatures(player); 
        // mark chunk as loaded
        loadedChunks.add(chunk);
      }
    }, this);

    // spawn humidity above ground
    Random rng = new Random();
    Set<Block> humids = new HashSet<Block>();
    Rain.start(() -> {
      for (Player player : Rain.server.getOnlinePlayers()) {
        // pick random position in chunk
        Chunk chunk = player.getLocation().getChunk();
        Block air = RainUtil
          .topAir(chunk, rng.nextInt(16), rng.nextInt(16))
          .getRelative(BlockFace.UP);
        // spawn humidity
        double humidity = Rain.config.spawnHumidity; 
        if (air.hasMetadata("humidity")) {
          humidity += air.getMetadata("humidity").get(0).asDouble();
        }
        air.setMetadata("humidity", new FixedMetadataValue(this, humidity));
        humids.add(air);
      } 
    }, 5l);

    // move humidity through the air
    Rain.start(() -> {
      Set<Block> toAdd = new HashSet<Block>();
      Set<Block> toDelete = new HashSet<Block>();
      for (Block block : humids) {
        moveHumidity(
          block, 
          nextBlock -> toAdd.add(nextBlock),
          nextBlock -> toDelete.add(nextBlock)
        );
        // (consider) activate any neighbors that have humidity by adding them into humids
      }
      for (Block block : toDelete) { humids.remove(block); }
      for (Block block : toAdd) { humids.add(block); }
    }, 1l);
  }

  void moveHumidity(Block block, Consumer<Block> onActivate, Consumer<Block> onStagnate) {
    if (!block.hasMetadata("humidity")) { throw new RuntimeException("humid has no humidity"); }
    double humidity = block.getMetadata("humidity").get(0).asDouble();
    // spread humidity
    int spread = 1;
    if (humidity > Rain.config.humiditySpreadThreshold) { spread = 2; }
    // move humidity
    Block[] nextBlocks = RainUtil.windDirection(block, spread);
    for (Block nextBlock : nextBlocks) {
      if (!RainUtil.isAir(nextBlock)) { throw new RuntimeException("humidity moved the wrong way; into ground"); }
      if (nextBlock == block) { onStagnate.accept(block); continue; }
      if (nextBlock.hasMetadata("humidity")) {
        double nextHumidity = nextBlock.getMetadata("humidity").get(0).asDouble();
        humidity += nextHumidity;
      }
      nextBlock.setMetadata("humidity", new FixedMetadataValue(this, humidity));
      onActivate.accept(nextBlock);
    }
    block.removeMetadata("humidity", this);
    onStagnate.accept(block);
  }

  // create a blue->red dome around the player to visualize temperature using stained glass
  private void loadTemperatures(Player player) {
    Chunk playerChunk = player.getLocation().getChunk();
    // select topmost air
    Set<Block> blocks = new HashSet<Block>();
    for (Block block : RainUtil.topAirs(playerChunk)) {
      blocks.add(block.getRelative(BlockFace.UP));
    }
    Rain.log(String.format("Found %d blocks to temper with", blocks.size()));
    Rain.log("assigning temperatures...");
    Set<Block> filled = new HashSet<Block>();
    assignTemperatures(playerChunk, blocks, block -> filled.add(block));
    for (Block block : filled) {
      Double temperature = block.getMetadata("temperature").get(0).asDouble();
      // render temperature with blocks
      //if (temperature > Rain.config.cloudMinTemperature && temperature < Rain.config.cloudMaxTemperature) {
      //  block.setType(Material.POWDER_SNOW);
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

  // pool temperature into blocks metadata and recurse neighbors
  // recursive algo; given blocks, apply temperature metadata
  private void assignTemperatures(Chunk chunk, Set<Block> blocks, Consumer<Block> onCreated) { 
    if (blocks.isEmpty()) { return; }
    int skipGround = 0;
    int skipTempered = 0;
    int skipOutsideChunk = 0;

    List<Runnable> postActions = new ArrayList<Runnable>();
    // collect neighbors
    Set<Block> unvisited = new HashSet<Block>();
    for (Block block : blocks) {
      // ignore solid blocks
      if (!RainUtil.isAir(block)) {
        skipGround++;
        continue;
      }
      if (block.hasMetadata("temperature")) {
        skipTempered++;
        continue;
      }

      // collect neighboring temperatures
      Block[] neighbors = RainUtil.neighbors(block);
      Double[] temperatures = new Double[neighbors.length];
      for (int i = 0; i < neighbors.length; i++) {
        temperatures[i] = RainUtil.temperaturize(neighbors[i]);
      }

      // pool average
      double averageTemperature = RainUtil.poolTemperature(temperatures);
      postActions.add(() -> {
        // update block temperature
        block.setMetadata("temperature", new FixedMetadataValue(this, averageTemperature));

        // back-propogate average
        Double[] newTemperatures = RainUtil.reversePoolTemperature(averageTemperature, temperatures);
        for (int i = 0; i < neighbors.length; i++) {
          Double temperature = newTemperatures[i];
          if (temperature == null) { continue; }
          neighbors[i].setMetadata("temperature", new FixedMetadataValue(this, temperature));
        }
        // advertise block
        onCreated.accept(block);
      });

      // make sure block is the same chunk
      if (!block.getChunk().equals(chunk)) {
        skipOutsideChunk++;
        continue;
      }

      // stop if temperature is too low
      if (averageTemperature < Rain.config.minTemperature) { continue; }

      // find unvisited neighbors
      for (Block neighbor : neighbors) {
        unvisited.add(neighbor);
      }
    }

    // log skips
    if (skipGround > 0) { Rain.log(String.format("Skipped %d ground blocks", skipGround)); }
    if (skipOutsideChunk > 0) { Rain.log(String.format("Skipped %d blocks outside chunk", skipOutsideChunk)); }
    if (skipTempered > 0) { Rain.log(String.format("Skipped %d tempered blocks", skipTempered)); }

    // complete postActions 
    for (Runnable action : postActions) { action.run(); }

    Rain.log(String.format("Assigned temperatures to %d blocks", blocks.size()));
    Rain.log(String.format("%d blocks to go", unvisited.size())); 

    // recurse
    assignTemperatures(chunk, unvisited, onCreated);
  }
}
