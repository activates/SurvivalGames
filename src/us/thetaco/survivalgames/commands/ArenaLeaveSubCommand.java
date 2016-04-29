package us.thetaco.survivalgames.commands;

import org.bukkit.entity.Player;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.Values;

/** Used for the management of the arena leave subcommand
 * @author activates
 *
 */
public class ArenaLeaveSubCommand {

	/** Used to handle the leave subcommand. The arena will be found out automatically
	 * @param p The player to remove
	 */
	public void removePlayer(Player player) {
		
		String arenaName = "";
		
		if ((arenaName = SurvivalGames.getLobbyHandler().getPlayerArenaName(player)) != null) {
			
			if (SurvivalGames.getLobbyHandler().removePlayerFromLobby(arenaName, player)) {
				
				player.sendMessage(Language.SUCESSFULLY_REMOVED.toString());
				
			} else {
				
				player.sendMessage(Language.UNSUCESSFULLY_REMOVED.toString());
				
			}
			
		} else if ((arenaName = SurvivalGames.getArenaData().playerInArena(player, false)) != null) {

			if (SurvivalGames.getArenaData().removePlayerFromArena(arenaName, player)) {
				
				player.sendMessage(Language.SUCESSFULLY_REMOVED.toString());
				
				// messaging other players in that arena that the certain player has been removed
				SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.LEAVE_BROADCAST.toString(null, player, null, null), false);
				
				// restoring the player's inventory
				SurvivalGames.getArenaData().restoreInventory(player);
				
				// getting rid of that annoying scoreboard!
				SurvivalGames.getArenaHandler().removeScoreboard(player);

				// move the player to the lobby
				SurvivalGames.getArenaData().movePlayerToLobby(arenaName, player);
				
				// handle reputation
				if (Values.REPUTATION_ENABLED) {
					
					// minus their reputation
					SurvivalGames.getArenaData().minusReputation(player);
					
					// tell the player their reputation has been harmed!
					player.sendMessage(Language.LEAVING_HARMED_REP.toString(null, player, null, null));
					
					if (Values.BROADCAST_MINUS_REP) {
						
						SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.LEAVING_HARMED_REP.toString(null, player, null, null), false);
						
					}
					
				}
				
				// handling the player being removed from the arena
				
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
				
			} else {
				
				player.sendMessage(Language.UNABLE_TO_REMOVE.toString());
				
			}
			
			
			
		}
			
	}
	
}
