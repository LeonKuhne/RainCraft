package org.rainworld.command;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Consumer;

public class RainCommand extends Command {
  protected Function<Info, Boolean> action;
  private RainCommand() { 
    super("", "", "", new ArrayList<String>());
    action = null;
  }
  public static RainCommand create(String name) {
    RainCommand builder = new RainCommand();
    builder.setName(name);
    return builder;
  }

  public class Info {
    public CommandSender sender;
    public Player player;
    public List<String> args;
    public Info(CommandSender sender, List<String> args) {
      this.sender = sender;
      this.args = args;
    }
  }

  //
  // DETAILS

  public RainCommand setDescription(String description) {
    super.setDescription(description);
    return this;
  }
  public RainCommand setUsage(String usage) {
    super.setUsage(usage);
    return this;
  }
  public RainCommand setAliases(List<String> aliases) {
    super.setAliases(aliases);
    return this;
  }
  public RainCommand setPermissions(String permission) {
    super.setPermission(permission);
    return this;
  }

  //
  // ACTIONS 

  public RainCommand anyone(Function<Info, Boolean> action) {
    this.action = action;
    return this;
  }
  public RainCommand anyone(Runnable action) {
    this.action = (info) -> { action.run(); return true; };
    return this;
  }
  public RainCommand anyone(Consumer<Info> action) {
    this.action = (info) -> { action.accept(info); return true; };
    return this;
  }
  public RainCommand player(Consumer<Info> action) {
    this.anyone((info) -> {
      if (!(info.sender instanceof Player)) {
        info.sender.sendMessage("You must be a player to use this command");
        return false;
      }
      info.player = (Player) info.sender;
      action.accept(info);
      return true;
    });
    return this;
  }

  // 
  // EXECUTE 

  @Override
  public boolean execute(CommandSender sender, String label, String[] args) {
    if (action == null) return false;
    return action.apply(new Info(sender, new ArrayList<String>(Arrays.asList(args))));
  }
}