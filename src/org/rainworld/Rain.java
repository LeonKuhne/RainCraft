package org.rainworld;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class Rain {
  public static Plugin plugin;
  public static Logger logger;
  public static Server server;
  public static BukkitScheduler scheduler;
  public static List<BukkitTask> tasks;
  public static List<Player> subscribers;
  public static Config config;
  public static List<String> lastMessages;

  public static void init(Plugin plugin) {
    Rain.plugin = plugin;
    logger = plugin.getLogger();
    server = plugin.getServer();
    scheduler = Rain.server.getScheduler();
    tasks = new ArrayList<BukkitTask>();
    subscribers = new ArrayList<Player>();
    config = new Config();
    lastMessages = new ArrayList<String>();
    // sort colors by threshold
    Rain.config.colors.sort((a, b) -> a.getValue().compareTo(b.getValue()));
  }

  // LOGGING
  //

  public static void listen(Player player) {
    if (!subscribers.contains(player)) {
      subscribers.add(player);
    }
  }
  public static void ignore(Player player) {
    if (subscribers.contains(player)) {
      subscribers.remove(player);
    }
  }
  public static void log(String msg) {
    if (lastMessages.contains(msg)) return;
    logger.info(msg);
    subscribers.forEach(player -> player.sendMessage(msg));
    lastMessages.add(msg);
    if (lastMessages.size() > Rain.config.duplicateHistory) {
      lastMessages.remove(0);
    }
  }

  // THREADING
  //  

  public static void start(Runnable callback, long period) {
    tasks.add(
      scheduler.runTaskTimer(plugin, callback, 0l, period));
  }

  public static void stop() {
    log("The rain is ending.");
    // stop tasks
    while (tasks.size() > 0) {
      tasks.remove(0).cancel();
    }
  }

  public static boolean configure(CommandSender sender, List<String> args) {
    if (args.size() == 0) {
      sender.sendMessage(config.toString());
      return false;
    }
    // update config based on args
    for (int i = 0; i < args.size(); i++) {
      String key = args.get(i);
      String value = args.get(i + 1);
      // check if key exists in config
      if (config.has(key)) {
        config.set(key, value);
      } else {
        log("Unknown config key: " + key);
        return false;
      }
      i++;
    }
    return true;
  }
}
