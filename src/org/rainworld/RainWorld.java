package org.rainworld;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.rainworld.command.RainCommand;

public class RainWorld extends JavaPlugin {

  private SimpleCommandMap commands;

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
      RainCommand.create( "heat").player(info -> { showTemperatures(info.player); }),
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
    /* TOO SLOW
    Rain.server.getPluginManager().registerEvents(new Listener() {
      @EventHandler
      public void onChunkLoad(ChunkLoadEvent event) {
        Rain.log(String.format("Chunk loaded: %s", event.getChunk().toString()));
        Rain.log("Finding solids...");
        Set<Block> blocks = RainUtil.allBlocks();
        Rain.log("Assigning temperatures...");
        assignTemperatures(blocks);
      }
    }, this);
    */

    // TODO hook into block create/destroy to propogate temperature (using different algo)
  }

  // create a blue->red dome around the player to visualize temperature using stained glass
  private void showTemperatures(Player player) {
    // assign temperatures to the players chunk and surrounding chunks
    Chunk playerChunk = player.getLocation().getChunk();
    // select nearby chunks
    Set<Chunk> chunks = new HashSet<Chunk>();  
    chunks.add(playerChunk); 
    chunks.addAll(Arrays.asList(RainUtil.neighbors(playerChunk)));
    Set<Block> blocks = new HashSet<Block>();
    for (Chunk chunk : chunks) { 
      for (Block block : RainUtil.topBlocks(chunk)) {
        blocks.add(block.getRelative(BlockFace.UP));
      }
      // TOO SLOW
      //blocks.addAll(RainUtil.allBlocks(chunk));
    }
    // load topmost blocks
    Rain.log(String.format("Found %d blocks to temper with", blocks.size()));
    Rain.log("assigning temperatures...");
    assignTemperatures(blocks);
  }

  private void debugTemperature(Block block) {
    if (!block.hasMetadata("temperature")) { return; }
    double temperature = block.getMetadata("temperature").get(0).asDouble();
    Rain.log(String.format("Found block with temperature: %f", temperature));
    // sort colors by threshold
    Rain.config.colors.sort((a, b) -> a.getValue().compareTo(b.getValue()));
    // visualize temperature; assumes colors are sorted smallest threshold to largest
    for (Map.Entry<String, Double> entry : Rain.config.colors) {
      if (temperature < entry.getValue()) { 
        block.setType(Material.getMaterial(entry.getKey()));
        break;
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
  private void assignTemperatures(Set<Block> blocks) {
    assignTemperatures(blocks, new HashSet<Block>());
  }
  private void assignTemperatures(Set<Block> blocks, Set<Block> visited) {
    if (blocks.isEmpty()) { return; }

    List<Runnable> postActions = new ArrayList<Runnable>();
    // collect neighbors
    Set<Block> unvisited = new HashSet<Block>();
    for (Block block : blocks) {
      // ignore solid blocks
      if (!RainUtil.isAir(block)) { continue; }

      // capture surrounding temperature and mark for next round
      double totalTemperature = 0.0;
      Block[] neighbors = RainUtil.neighbors(block);
      for (Block neighbor : neighbors) {
        if (neighbor.hasMetadata("temperature")) { 
          totalTemperature += neighbor.getMetadata("temperature").get(0).asDouble();
          continue; 
        }
        if (!RainUtil.isAir(neighbor)) {
          totalTemperature += 1;
          continue;
        }
      }

      // assign average temperature to block
      double averageTemperature = totalTemperature / 6.0;
      postActions.add(() -> {
        block.setMetadata("temperature", new FixedMetadataValue(this, averageTemperature));
        debugTemperature(block);
      });

      // find unvisited neighbors
      for (Block neighbor : neighbors) {
        if (!RainUtil.isAir(neighbor) || neighbor.hasMetadata("temperature")) { continue; }
        // make sure neighbors are in the same chunk
        if (!RainUtil.sameChunk(block, neighbor)) { continue; }
        unvisited.add(neighbor);
      }
    }

    // complete postActions 
    for (Runnable action : postActions) { action.run(); }

    // stop if too cold
    if (!blocks.stream().anyMatch(block -> {
      double temperature = block.getMetadata("temperature").get(0).asDouble();
      return temperature > Rain.config.minTemperature;
    })) {
      return;
    }

    // recurse
    visited = blocks;
    assignTemperatures(unvisited, visited);
  }
}
