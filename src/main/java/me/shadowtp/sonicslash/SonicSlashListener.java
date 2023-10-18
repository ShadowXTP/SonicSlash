package me.shadowtp.sonicslash;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;


public class SonicSlashListener implements Listener {

    private static final long REQUIRED_SNEAK_DURATION = 1000;

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer.canBend(CoreAbility.getAbility(SonicSlash.class))) {
            new SonicSlash(player);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        SonicSlash SonicSlash = CoreAbility.getAbility(player, SonicSlash.class);

        if (event.isSneaking()) {
            // Player pressed the shift key.
            if (SonicSlash != null) {
                SonicSlash.sneaking = true;
                SonicSlash.sneakStartTime = System.currentTimeMillis();
            }
        } else {
            // Player released the shift key.
            if (SonicSlash != null) {
                long sneakDuration = System.currentTimeMillis() - SonicSlash.sneakStartTime;
                if (sneakDuration < REQUIRED_SNEAK_DURATION) {
                    // Enter tracking mode if sneak duration is short.
                    SonicSlash.setTrackingMode(true);
                } else {
                    // Remove the ability if sneak duration exceeds 1 second.
                    SonicSlash.remove();
                }
            }
        }
    }

}
