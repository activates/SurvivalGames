package us.thetaco.survivalgames.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import us.thetaco.survivalgames.SurvivalGames;

/** A class for handling player movement in arenas.. It will basically just prevent them from moving away from that set location
 * @author activates
 *
 */
public class PlayerMovementListener implements Listener {

	// will hold the hash map of the location to teleport for that specific player
	private Map<String,Location> teleportLocation = new HashMap<String, Location>();
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerMovement(PlayerMoveEvent e) {
		
		Player player = e.getPlayer();
		
		Location teleportLocation = this.getPlayerTeleportLocation(player.getName());
		
		// stop the function right here if the player is not frozen
		if (!SurvivalGames.getArenaData().isPlayerFrozen(e.getPlayer()) && teleportLocation != null) {
			
			// check to make sure the player is still supposed to be frozen
			this.removePlayerTeleportLocation(player.getName());
			return;
			
		} else if (!SurvivalGames.getArenaData().isPlayerFrozen(player)) {
			return;
		}
		
		if (teleportLocation == null) {
			
			this.setPlayerTeleportLocation(player.getName(), player.getLocation());
			player.teleport(this.getPlayerTeleportLocation(player.getName()));
			e.setCancelled(true);
			return;
			
		}
		
		// if the location exists, we will just teleport the player to that location, then cancel the event
		player.teleport(teleportLocation);
		e.setCancelled(true);
		
	}
	
	/** Used to add the location to hold that player too..
	 * @param playerName The name of the player that will be teleported
	 * @param loc The desired teleport location
	 */
	private void setPlayerTeleportLocation(String playerName, Location loc) {
		
		teleportLocation.put(playerName.toLowerCase(), loc);
		
	}
	
	/** Used to remove a player's teleport location
	 * @param playerName The player to remove the teleport location
	 */
	private void removePlayerTeleportLocation(String playerName) {
		
		teleportLocation.remove(playerName.toLowerCase());
		
	}
	
	/** Used to get the location that the specified player will be teleported.. will return null if no location is stored for the
	 * specified player
	 * @param playerName The player to get the player teleport location
	 * @return Will return the stored teleport location for the specified player. will return null if no location is stored
	 */
	private Location getPlayerTeleportLocation(String playerName) {
		
		return this.teleportLocation.get(playerName.toLowerCase());
		
	}
	
}
