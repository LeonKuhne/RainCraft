package org.rainworld;
import java.util.Arrays;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.rainworld.command.RainCommand;
import org.rainworld.physics.ParticleEngine;

public class RainWorld extends JavaPlugin {


  private ParticleEngine engine;
  private SimpleCommandMap commands;

  @Override
  public void onEnable() {
    Rain.init(this);
    //clouds = new ArrayList<Cloud>();
    //spawning = true;
    engine = new ParticleEngine();

    //
    // COMMANDS 
    commands = new SimpleCommandMap(Rain.server);
    commands.registerAll("rain", Arrays.asList(
      RainCommand.create("cloud").player(info -> placeCloudAbove(info.player, null)),
      RainCommand.create("stop").anyone(info -> {destroyClouds();}),
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
    Rain.start(() -> engine.tick(), Rain.config.ticksPerSpawn);
  }

  @Override
  public void onDisable() {
    Rain.stop();
    destroyClouds();
  }

  private void destroyClouds() {
    engine.particles.clear();
  }

  private void placeCloudAbove(Player player, Map<String, Double> cloudFactors) {
    Location playerLocation = player.getLocation();
    Vector pos = RainUtil.cloudAbove(playerLocation).toVector();
    player.sendMessage(String.format("Placing a cloud above %s at %s", player.getName(), pos)); 
    engine.addParticles(pos, Rain.config.particlesPerSpawn);
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

  /* legacy 
  public void generateClouds() {
    // near each player
    for (Player player : RainUtil.getOverworld().getPlayers()) {
      if (clouds.size() < Rain.config.maxClouds && spawning) {
        Location groundLoc = RainUtil.RandomNear(player.getLocation(), Rain.config.cloudSpawnWidth);
        Location cloudLoc = RainUtil.cloudAbove(groundLoc);

        // cloud is high enough
        if (RainUtil.isChosen(cloudLoc.getY(), Rain.config.cloudHeight, Rain.config.cloudSpawnThreshold)) {
          Rain.log("Cloud formed!");
          clouds.add(new Cloud(this, cloudLoc));
        }
      }
    }
  }

  private Map<String, Double> parseFactors(String[] args) {
    List<String> argFactors = new ArrayList<String>(Arrays.asList(args));
    argFactors.remove(0); // remove command identifier
    return parseArgMap(argFactors);
  }

  private Map<String, Double> parseArgMap(List<String> args) {
    return new HashMap<String, Double>() {
      {
        while (args.size() > 1) {
          try {
            Double factor = Double.parseDouble(args.remove(0));
            put(args.remove(0), factor);
          } catch (NumberFormatException e) {
            Rain.log("Failed to parse args, not a number");
          }
        }
      }
    };
  }
  */
}
