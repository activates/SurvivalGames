package us.thetaco.survivalgames.listeners;

import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.Values;

/** A class for handling player deaths etc.. Mainly for removing them from matches
 * @author activates
 *
 */
public class PlayerDeathListener implements Listener {
	
	private PlayerRespawnListener pRespawnListener;
	public PlayerDeathListener(PlayerRespawnListener pRespawnListener) {
		this.pRespawnListener = pRespawnListener;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDeath(PlayerDeathEvent e) {
		
		if (e.getEntityType() != EntityType.PLAYER) return;
		
		Player player = e.getEntity();
		
		// even though quite a useless check, we are going to check if the player was spectating when they died
		String spectatingArenaName = SurvivalGames.getArenaData().isSpectating(player);
		
		if (spectatingArenaName != null) {
			
			player.sendMessage(Language.DIED_WHILE_SPECTATING.toString());
			player.setGameMode(GameMode.SURVIVAL);
			
			SurvivalGames.getArenaData().movePlayerToLobby(spectatingArenaName, player);
			SurvivalGames.getArenaData().removeSpectator(spectatingArenaName, player);
			
		}
		
		// checking if the player is in a lobby.. we'll remove them if they are
		
		String lobbyName = SurvivalGames.getLobbyHandler().getPlayerArenaName(player);
		
		if (lobbyName != null) {
			
			// if the player died while in the lobby, remove them from it
			SurvivalGames.getLobbyHandler().removePlayerFromLobby(lobbyName, player);
			
			// message all the players that the player has been removed from the game
			SurvivalGames.getLobbyHandler().messagePlayersInLobby(lobbyName, Language.KILLED_AND_REMOVED.toString(null, player, null, null));
			
			return;
		}
		
		
		String arenaName = SurvivalGames.getArenaData().playerInArena(player, false);
		
		if (arenaName != null) {
		
			// set the player as dead and remove them from the game
			SurvivalGames.getArenaData().setPlayerDead(arenaName, player, true);
						
			// Striking the player with lightning to notify other players
			Location pLocation = player.getLocation();
						
			if (Values.LIGHTNING_ENABLED) {
			
				pLocation.getWorld().strikeLightningEffect(pLocation);
			
			}
			
			// adding the player's reputation if they earned it and it's enabled
			if (Values.REPUTATION_ENABLED) {
				
				// doing some math on if they earned the reputation by being in the game long enough
				int gameTimeSeconds = SurvivalGames.getArenaData().getArenaTime(arenaName);
				int currentGameTimeSeconds = SurvivalGames.getArenaHandler().getCurrentGameTime(arenaName);
				
				int timePassed = gameTimeSeconds - currentGameTimeSeconds;
				
				if (timePassed >= Values.MINIMUM_REP_ADD) {
					
					// this will run if the player DID earn their positive reputation point
					player.sendMessage(Language.DEATH_REP_EARNED.toString());
					
					SurvivalGames.getArenaData().addReputation(player);
					
				} else {
					
					// this will run if the player DIDN'T earn their positive reputation point. It not will strike against their account, though
					player.sendMessage(Language.DEATH_REP_NOT_EARNED.toString());
					
				}
				
			}
			
			/*
			 * A big part of the game is handled here.. if there is only one player left after this player is killed, then the game is over!
			 * If there is only one player, this will never happen and the game will run itself out (or the player leaves)
			 */
			
			int alivePlayerCount = SurvivalGames.getArenaData().getAmountOfPlayerInArena(arenaName, false);
			
			if (alivePlayerCount < 2) {
				
				// This will run if there is only one player left (or there was only one player in the arena and they died
				
				// adding the player to be teleported on respawn
				pRespawnListener.addPlayerToMove(player);
				
				// removing their scoreboard
				SurvivalGames.getArenaHandler().removeScoreboard(player);
				
				Set<String> remainingPlayers = SurvivalGames.getArenaData().getPlayersInArena(arenaName, false);
				
				if (remainingPlayers != null && remainingPlayers.size() > 0) {
					
					// run this if there is at least one player left
					SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.KILLED_AND_REMOVED.toString(arenaName, null, null, player.getName()), true);
					
				} else {
					
					// run this if there are no players
					SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.GAME_WON.toString(arenaName, player, null, null), true);
					
				}
				
				
				// clear the arena
				SurvivalGames.getArenaHandler().closeGame(arenaName);
				
				// we will stop it here and handle everything in a special way since the arena is going to be closed
				return;
				
			}
			
			// message all players still alive that the specified player (in this event) has been killed
			
			SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.KILLED_AND_REMOVED.toString(null, player, null, null), false);
			
			// adding the player to be teleported on respawn
			pRespawnListener.addPlayerToMove(player);
			
			// removing their scoreboard
			SurvivalGames.getArenaHandler().removeScoreboard(player);
			
		}
	}
	
}
