package us.thetaco.survivalgames.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.Values;

/** A class that acts as a sort of filter or floodgate for player commands when they are in an arena. 
 * @author activates
 *
 */
public class PlayerCommandListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
		
		// If all commands are allowed.. just stop here
		if (Values.ALLOW_COMMANDS || e.isCancelled()) return;
		
		Player player = e.getPlayer();
		
		if (SurvivalGames.getArenaData().playerInArena(player, false) == null && SurvivalGames.getArenaData().isSpectating(player) == null) {
			// just stop here since the player isn't actively in any arena (as a live player or a spectator)
			return;
		}
		
		String[] formattedCommand = e.getMessage().split(" ");
		
		// check to see if the command name matches any of the allowed ones, if it doesn't, cancel the even and message the player
		for (String s : Values.BYPASSING_COMMANDS) {
			
			if (formattedCommand[0].equalsIgnoreCase("/" + s)) {
				
				// stop the method here, since we don't want to cancel the event
				return;
				
			}
			
		}
		
		// if we get to this point, we will cancel the event and tell the player that they are not able to run this command
		player.sendMessage(Language.COMMAND_DISALLOWED.toString());
		e.setCancelled(true);
		
	}
	
}
