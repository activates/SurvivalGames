package us.thetaco.survivalgames.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;

/** Used to respawn a player at an arena's lobby if they died in it..
 * @author activates
 *
 */
public class PlayerRespawnListener implements Listener {

	private List<String> playersToMove = new ArrayList<String>();
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		
		final Player player = e.getPlayer();
		
		String arenaName = SurvivalGames.getArenaData().playerInArena(player, true);
				
		// just stop here if the arena is null
		if (arenaName == null) {
			return;
		}
		
		// check to see if the player needs to move
		if (!this.playerNeedsMove(player)) {
			return;
		}
		
		Location lobbyLoc = SurvivalGames.getArenaData().getArenaLobby(arenaName);
		
		// teleport the player to the lobby and remove them from the players that need to be teleported
		e.setRespawnLocation(lobbyLoc);
		this.removePlayerToMove(player);
		
		player.sendMessage(Language.INVENTORY_RESTORING.toString());
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin(SurvivalGames.getPluginName()), new Runnable() {
			public void run() {
				
				// set the player's inventory to what it was before they died
				SurvivalGames.getArenaData().restoreInventory(player);
				
			}
		}, 100L);
		
	}
	
	/** Used to add players who need to be moved to the arena lobby spawnpoint
	 * @param playerName The player in need of a movement
	 */
	public void addPlayerToMove(Player player) {
		
		String playerName = player.getName().toLowerCase();
		
		if (this.playerNeedsMove(player)) return;
		
		this.playersToMove.add(playerName);
		
	}
	
	/** Used to determine if a player needs to be teleported to the arena lobby
	 * @param playerName The player in question
	 * @return Will return true if the player has not been moved to the spawn area, yet.
	 */
	private boolean playerNeedsMove(Player player) {
		
		String playerName = player.getName().toLowerCase();
		
		boolean doesContain = false;
		
		for (String s : this.playersToMove) {
			
			if (playerName.equalsIgnoreCase(s)) {
				doesContain = true;
				break;
			}
			
		}
		
		return doesContain;
		
	}
	
	/** Used to remove a player from the teleport list after they have been teleported (or it was cancelled for some reason)
	 * @param player The player to remove from the list
	 */
	private void removePlayerToMove(Player player) {
		
		String playerName = player.getName().toLowerCase();
		
		this.playersToMove.remove(playerName);
		
	}
	
}
