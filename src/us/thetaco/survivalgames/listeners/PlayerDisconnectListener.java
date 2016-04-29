package us.thetaco.survivalgames.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.commands.AdminCommand;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.Values;

/** A class that simply cleans up after the player if they were in any type of arena-creation mode
 * This is just to prevent memory leaks etc
 * @author activates
 *
 */
public class PlayerDisconnectListener implements Listener {

	private AdminCommand adminCommand;
	private PlayerChatListener pChatListener;
	public PlayerDisconnectListener(AdminCommand adminCommand, PlayerChatListener pChatListener) {
		this.adminCommand = adminCommand;
		this.pChatListener = pChatListener;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDisconnect(PlayerQuitEvent e) {
		
		// simply cleaning up after players.. even if they weren't in any list
		Player player = e.getPlayer();
		
		String arenaCreationName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
		
		if (arenaCreationName != null) {
			pChatListener.removeMPCount(arenaCreationName);
			pChatListener.removeLobbyLocation(arenaCreationName);
			pChatListener.removeLobbyTime(arenaCreationName);
			pChatListener.removeArenaTime(arenaCreationName);
			pChatListener.removedLoadedPlayer(player);
			pChatListener.removeCenterLocation(arenaCreationName);
		}
		
		adminCommand.removePlayerAndArena(player.getName());
		adminCommand.removePlayersInArenaMode(player);
		adminCommand.removePlayerUpdating(player);
		
		// BEGIN checking if player was in an arena
		
		String spectatorArena = SurvivalGames.getArenaData().isSpectating(player);
		
		if (spectatorArena != null) {
			
			// if the player is a spectator, teleport them to the lobby, then remove them as a spectator
			SurvivalGames.getArenaData().movePlayerToLobby(spectatorArena, player);
			SurvivalGames.getArenaData().removeSpectator(spectatorArena, player);
			
		}
		
		String arenaName = SurvivalGames.getArenaData().playerInArena(player, false);
		
		if (arenaName == null) {
			// checking to see if a player is in an arena
			
			// also checking to make sure the player isn't in a lobby
			String lobbyName = SurvivalGames.getLobbyHandler().playerInLobby(player);
			
			// just.. stopping here
			if (lobbyName == null) return;
			
			// removing the player from the lobby
			SurvivalGames.getLobbyHandler().removePlayerFromLobby(lobbyName, player);
			
			SurvivalGames.getLobbyHandler().messagePlayersInLobby(lobbyName, Language.LEAVE_BROADCAST.toString(null, player, null, null));
			
			return;
		}
		
		/*
		 * This will run if the arena is not null...
		 * First we need to check if the player is dead. If they are, we will do nothing, because dead players don't
		 * technically count as part of the arena anymore (but they are still stored in it)
		 */
		
		if (SurvivalGames.getArenaData().isPlayerDead(arenaName, player)) {
			
			// if the player is dead.. we will just stop this here because we don't need to remove them!
			
			// for now, we will send a message to the players in the arena
			SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.DEAD_BUT_DISCONNECTED.toString(null, player, null, null), false);
			
			return;
			
		}
		
		// handing the player's reputation if it's enabled
		if (Values.REPUTATION_ENABLED) {
			
			// decrementing the player's reputation because they quit the game
			SurvivalGames.getArenaData().minusReputation(player);
			
			// broadcasting to the other players that the disconnecting player has had their reputation negatively impacted
			if (Values.BROADCAST_MINUS_REP) {
				
				SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.DISCONNECTING_HARMED_REP.toString(null, player, null, null), false);
				
			}
			
		}
		
		// moving the disconnected player back to the arena lobby
		SurvivalGames.getArenaData().movePlayerToLobby(arenaName, player);
		
		// restoring the player's inventory
		SurvivalGames.getArenaData().restoreInventory(player);
		
		// removing the player from the arena
		SurvivalGames.getArenaData().removePlayerFromArena(arenaName, player);
		
		SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.LEAVE_BROADCAST.toString(null, player, null, null), false);
		
		// making sure that the arena count still meets the minimum player count
		
		if (SurvivalGames.getArenaData().getMinimumPayerCount(arenaName) > SurvivalGames.getArenaData().getAmountOfPlayerInArena(arenaName, true)) {
			
			/*
			 * Here, we will check to see if the config allows this, and if the game is past the point of no return! (config option, too!)
			 */
			
			// if the no return option is disabled
			if (!Values.USE_NO_RETURN) {
			
				// closing down the arena because there are not enough players to continue the game
				
				// tell all players in the game that the game has been cancelled
				SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.LOW_PLAYER_COUNT_GAME_CANCELLED.toString(), false);
				
				// close down the arena here because it is still active, but no players are in it..
				SurvivalGames.getArenaHandler().closeGame(arenaName);
				
			} else {
				
				// if the no return option is enabled, we will check to make sure if the time allows it
				
				if (SurvivalGames.getArenaHandler().getCurrentGameTime(arenaName) <= Values.NO_RETURN_SECONDS) {
					
					// this will run if the current game time in the specified lobby is at or below the point of no return
					// if it's at this point, we do nothing.. simple, right?
					
					SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.LOW_PLAYER_COUNT_GAME_NOT_CANCELLED.toString(), false);
					
				} else {
					
					// if it's at this point, then the game can still be cancelled.. so we will cancel it
					
					// tell all players in the game that the game has been cancelled
					SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.LOW_PLAYER_COUNT_GAME_CANCELLED.toString(), false);
					
					// close down the arena here because it is still active, but no players are in it..
					SurvivalGames.getArenaHandler().closeGame(arenaName);
					
				}
				
				
			}
			
		}
		
		// checking to make sure the arena still has players in it
		if (SurvivalGames.getArenaData().isArenaActive(arenaName) && SurvivalGames.getArenaData().getAmountOfPlayerInArena(arenaName, true) < 1) {
			
			// close down the arena here because it is still active, but no players are in it..
			SurvivalGames.logger().logMessage(Language.ARENA_ABANDONED.toString(arenaName, null, null, null));
			SurvivalGames.getArenaHandler().closeGame(arenaName);
			
		}
		
		
	}
	
}
