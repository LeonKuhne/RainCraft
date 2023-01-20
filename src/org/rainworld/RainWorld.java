package org.rainworld;
import static org.rainworld.RainUtil.CLOUD_SPAWN_WIDTH;
import static org.rainworld.RainUtil.MAX_CLOUDS;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RainWorld extends JavaPlugin {
  static final double CLOUD_SPAWN_THRESHOLD = 0.95;

  List<Cloud> clouds;
  boolean spawning;

  @Override
  public void onEnable() {
    getLogger().info("The rain is starting.");
    clouds = new ArrayList<Cloud>();
    spawning = true;

    // check if player under cloud
    getServer().getPluginManager().registerEvents(
      new PlayerUnderCloud(), this);

    // create clouds
    this.getServer().getScheduler().runTaskTimer(
      this, () -> generateClouds(), 50l, 5l);
  }

  @Override
  public void onDisable() {
    getLogger().info("The rain is ending.");
    destroyClouds();
  }

  public void generateClouds() {
    // generate clouds for each player
    for (Player player : RainUtil.getOverworld().getPlayers()) {
      if (clouds.size() < MAX_CLOUDS && spawning) {
        Location groundLoc = RainUtil.RandomNear(player.getLocation(), CLOUD_SPAWN_WIDTH);
        Location cloudLoc = RainUtil.cloudAbove(groundLoc);

        // try generate
        if (RainUtil.isChosen(cloudLoc.getY(), RainUtil.CLOUD_HEIGHT, CLOUD_SPAWN_THRESHOLD)) {
          getLogger().info("Cloud formed!");

          // place a cloud
          clouds.add(new Cloud(this, cloudLoc));
        }
      }
    }
  }

  private void destroyClouds() {
    while (clouds.size() > 0) {
      clouds.remove(0).destroy();
    }
  }

  private void placeCloudAbove(Player player, Map<String, Double> cloudFactors) {
    // get player xyz
    Location playerLocation = player.getLocation();
    player.sendMessage("You are at " + playerLocation);

    // place a cloud
    player.sendMessage("Placing a cloud above you " + player.getDisplayName());
    clouds.add(new Cloud(this, player, cloudFactors));
  }

  private Map<String, Double> parseCommand(String[] args) {
    List<String> argFactors = new ArrayList(Arrays.asList(args));
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
            getLogger().info("Failed to parse args, not a number");
          }
        }
      }
    };
  }

  @Override
  public boolean onCommand(
    CommandSender sender, Command cmd, String label, String[] args
  ) {
    if (label.equalsIgnoreCase("clouds")) {
      if (sender instanceof Player) {
        if (args.length == 0) {
          sender.sendMessage("RainCraft Commands:");
          sender.sendMessage("/clouds stop");
          sender.sendMessage(String.format(
              "/clouds spawn [threshold %1.2f, top %1.2f, bottom %1.2f, sides %1.2f, corners %1.2f]",
              Cloud.DEFAULT_THRESHOLD,
              Cloud.DEFAULT_TOP,
              Cloud.DEFAULT_BOTTOM,
              Cloud.DEFAULT_SIDES,
              Cloud.DEFAULT_CORNERS));
          return false;
        }

        // player
        Player player = (Player) sender;
        if (player.hasPermission("cloud.use")) { // check if player is op
          switch (args[0]) {
            case "spawn":
              getLogger().info("Spawning cloud");

              // parse args
              Map<String, Double> factors = args.length > 2 ? parseCommand(args) : null;
              placeCloudAbove(player, factors);

              break;
            case "stop":
              getLogger().info("Destroying clouds");
              spawning = false;
              destroyClouds();
              break;
            case "start":
              getLogger().info("Creating clouds");
              spawning = true;
              break;
            default:
              getLogger().info("Unknown command: " + args[0]);
              break;
          }

          return true;
        } else {
          player.sendMessage(ChatColor.RED + "you don't have permissions to place clouds");
        }
      } else {
        // console
        sender.sendMessage("you must be a player to execute /rain");
        return true;
      }
    }
    return false;
  }
}
