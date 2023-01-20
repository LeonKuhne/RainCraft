package org.rainworld;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerUnderCloud implements Listener {
    static final double LIGHTNING_STRIKE_CHANCE = 0.1;
    
    // get the clouds directly above the player
    public static List<Location> cloudsAbove(Location loc) {
        Location cursor = loc.clone();
        cursor.setY(cursor.getBlockY());
        
        List<Location> foundClouds = new ArrayList<Location>();
        
        // skip the air
        while (cursor.getBlockY() < cursor.getWorld().getMaxHeight()) {
            if (RainUtil.isCloud(cursor)) {
                foundClouds.add(cursor.clone());                                // its a cloud
            } else if (!RainUtil.isAir(cursor)) {
                return new ArrayList();                                         // its not air
            }
            cursor.add(0, 1, 0);                                                // move up
        }
        
        return foundClouds;
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // moved to different block
        if (!(RainUtil.sameLocation(event.getFrom(), event.getTo()))) {
            Location loc = event.getTo().clone();

            // check if raining
            if (cloudsAbove(loc).size() > 0) {
                // mess up vision
                //PotionEffect effect = new PotionEffect(PotionEffectType.BLINDNESS, MAX_RAIN_DELAY, 1);
                //event.getPlayer().addPotionEffect(effect);;
                
                // roll the dice on lightning
                if (Math.random() < LIGHTNING_STRIKE_CHANCE) {
                    event.getPlayer().getWorld().strikeLightning(event.getTo());
                }
                
                // make rain sounds
                //loc.getWorld().playSound(loc, Sound.WEATHER_RAIN_ABOVE, 0.5f, 0.5f);
            }
        }
    }
}
