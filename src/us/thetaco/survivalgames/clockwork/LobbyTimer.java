package us.thetaco.survivalgames.clockwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;

/** A class specifically for dealing with the timings of lobbies
 * @author activates
 *
 */
public class LobbyTimer {

	private SurvivalGames plugin;
	public LobbyTimer(SurvivalGames plugin) {
		this.plugin = plugin;
	}
	
	// a map for storing all the task ids for the lobby
	private Map<String, Integer> lobbyTimerIDs = new HashMap<String, Integer>();
	// a map for dealing with the amount of seconds before the actual game starts
	private Map<String, Integer> lobbyTime = new HashMap<String, Integer>();
	// a map for storing all players in a lobby
	private Map<String, List<String>> playersInLobby = new HashMap<String, List<String>>();
	
	public void startLobbyTimer(String arenaName, int waitTime) {
		
		int taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new LobbyHandler(waitTime, arenaName), 0L, 20L);
		
		lobbyTimerIDs.put(arenaName.toLowerCase(), taskID);
		
	}
	
	private class LobbyHandler implements Runnable {

		private int waitTime = 0;
		private String arenaName;
		public LobbyHandler(int waitTime, String arenaName) {
			this.waitTime = waitTime;
			this.arenaName = arenaName;
		}
		
		@Override
		public void run() {
			
			// make sure to cancel the lobby if no players are in it at all
			if (getPlayersInLobby(arenaName) != null && getPlayersInLobby(arenaName).size() < 1) {
				// Canceling the lobby if it is empty
				SurvivalGames.logger().logMessage(Language.LOBBY_ABANDONED.toString(arenaName, null, null, null));
				cancelLobby(arenaName);
				return;
			}
			
			// all will be handled in here from now on. This block of code will run every 20 ticks (roughly every 1 second)
			if (!enoughPlayersToStart(arenaName)) {
				
				// if this runs then there aren't enough players to start
				
				if (lobbyTime.get(arenaName.toLowerCase()) != null && lobbyTime.get(arenaName.toLowerCase()) != waitTime) {
					messagePlayersInLobby(arenaName, Language.LOBBY_TIMER_CANCELLED.toString());
				}
				
				// 'restarting the timer'
				lobbyTime.put(arenaName.toLowerCase(), waitTime);
				return;
			}
			
			// counting down on the timer
			Integer currentTime = lobbyTime.get(arenaName.toLowerCase());
			
			if (currentTime == null) {
				// this will run if the time is just being started. Since it hasn't, we'll initiate it here
				
				lobbyTime.put(arenaName.toLowerCase(), waitTime);
				return;
			}
			
			// checking to see if I can send the timer has started!
			if (currentTime == waitTime) {
				
				// this will run if this is the first time it is running
				messagePlayersInLobby(arenaName, ChatColor.GREEN + Language.LOBBY_TIMER_STARTED.toString());
				
			}
			
			if (currentTime > 0) {
				// this means that the lobby isn't ready to start, yet.. We will still check to see if we are in a time frame to notify the players of
				
				List<Integer> numbersToNotify = SurvivalGames.getArenaData().getNumbersToNotify();
				
				// checking to see if the numbers to notify actually checks the number correctly! :D
				boolean notify = false;
				for (Integer i : numbersToNotify) {
					if (i != null && currentTime == i) notify = true;
				}
				
				if (notify) {
					
					// if we reach this point, we will notify all the players in the arena of what time it is
					
					for (String s : getPlayersInLobby(arenaName.toLowerCase())) {
						
						Player inArena = plugin.getServer().getPlayer(s);
						
						// we need to check that the player didn't log out (we will also check in a disconnect listener)
						if (inArena == null) {
							
							removePlayerFromLobby(arenaName, s);
							
							// we also need to check that there are enough players...
							if (!SurvivalGames.getArenaData().areEnoughPlayersToStart(arenaName)) {
								// if we get to this point.. we know there aren't enough players to run the arena anymore
								cancelLobby(arenaName);
								return;
							}
							
						} else {
							
							// once again, just making sure there are enough players to start in order to count!
							
							// if we get to this point, we know the player exists, so we can send them the message
							inArena.sendMessage(Language.GAME_STARTING_TIMER.toString(null, null, currentTime, null));
							
						}
						
					}
					
					
				}
				
				// if we get to this point, we know we can start counting down here
				currentTime--;

				// "saving" my progress XD
				lobbyTime.put(arenaName.toLowerCase(), currentTime);
				
				return;
			} else {
				
				if (enoughPlayersToStart(arenaName)) {
				
					// this will run if the arena time has reached 0 (so it is time to start the game)
					startGame(arenaName);
					
				} else {
					
					// another fail-safe.. just in case
					lobbyTime.put(arenaName.toLowerCase(), waitTime);
					
				}
				
			}
			
		}
		
	}
	
	/** Starts the arena game.. It will take care of setting the variables and messaging the player. Will abort if the amount of players it below the minimum player count
	 * @param arenaName The name of the arena to start the game in
	 * @return Will return true if the game was started successfully 
	 */
	public boolean startGame(String arenaName) {
			
		// cancelling the active arena's lobby:
		this.cancelLobby(arenaName);
		
		List<String> playersInLobby = this.getPlayersInLobby(arenaName.toLowerCase());
		
		// looping through and messaging all the players that the game is starting.. we will also teleport the players to the spawns etc.. but that will be for another class!
		for (String s : playersInLobby) {
			
			// BEGIN messaging playrs
			Player inArena = plugin.getServer().getPlayer(s);
			
			// check if the players is null or not..
			if (inArena == null) {
				
				this.removePlayerFromLobby(arenaName, s);
				SurvivalGames.getArenaData().removePlayerFromArena(arenaName, s);
				
				// after removing the player object from the arena, we need to check and make sure there are still enough players to reach the minimum amount
				if (!SurvivalGames.getArenaData().areEnoughPlayersToStart(arenaName)) {
					
					/*
					 * TODO: handle not enough players for game to start.. probably just cancel it here.. but not sure yet
					 */
					
					// for now just clear the lobby
					this.clearLobby(arenaName);
					
					return false;
				}
				
			} else {
				
				
				// if we get to this point.. we can just simply tell the player the game has started
				inArena.sendMessage(Language.LOBBY_ENDED.toString());
				
			}
			
		}
		// END messaging players

		// sending the players over to the arena
		for (String s : this.getPlayersInLobby(arenaName)) {
		
			SurvivalGames.getArenaData().addPlayerToArena(arenaName.toLowerCase(), s.toLowerCase());
		
		}
		
		// for now.. just set the arena as active
		SurvivalGames.getArenaData().setArenaActive(arenaName, true);
		
		// moving all players to the arena
		SurvivalGames.getArenaData().distributePlayersToSpawns(arenaName);
		
		// calling the start of the arenaTimer
		// for now.. just manuall input the variables
		SurvivalGames.getArenaHandler().startArenaTimer(arenaName, 5, SurvivalGames.getArenaData().getArenaTime(arenaName));
		
		this.clearLobby(arenaName);
		
		return true;
		
	}
	
	/** A method for cancelling the looping task for the certain arena's lobby
	 * @param arenaName The arena to cancel the lobby timer
	 * @return Will return true if the arena had an active lobby and it was cancelled
	 */
	public boolean cancelLobby(String arenaName) {
		
		if (!this.activeLobby(arenaName.toLowerCase())) return false;
		
		Integer timerID = lobbyTimerIDs.get(arenaName.toLowerCase());
		
		if (timerID == null) {
			// check to see if the timer is null or not.. to avoid super errors :(
			return false;
		}
		
		Bukkit.getScheduler().cancelTask(timerID);
		
		this.lobbyTimerIDs.remove(arenaName.toLowerCase());
		this.lobbyTime.remove(arenaName.toLowerCase());
		return true;
		
	}
	
	/** A method used for checking if the lobby for an arena is active.
	 * @param arenaName The name of the arena to check if there is an active lobby
	 * @return Returns true if the lobby is active for the specified arena
	 */
	public boolean activeLobby(String arenaName) {
		
		if (this.lobbyTimerIDs.containsKey(arenaName.toLowerCase())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** A method for adding a player to the specified arena's lobby
	 * @param arenaName The arena to modify the lobby of
	 * @param player The player that needs to be added to the arena
	 */
	public void addPlayerToLobby(String arenaName, Player player) {
		
		// if (!this.activeLobby(arenaName)) return;
		
		if (!this.isPlayerInLobby(arenaName, player)) {
			
			List<String> playersInLobby = this.playersInLobby.get(arenaName);
			
			if (playersInLobby == null) {
				playersInLobby = new ArrayList<String>();
			}
			
			playersInLobby.add(player.getName().toLowerCase());
			
			this.playersInLobby.put(arenaName.toLowerCase(), playersInLobby);
			
		}
		
	}
	
	/** A method for removing the player from the lobby
	 * @param arenaName The name of the arena to remove the player from
	 * @param player The player who needs to be removed from the specified arena
	 * @return Will return true if the specified player was successfully removed from the arena's lobby
	 */
	public boolean removePlayerFromLobby(String arenaName, Player player) {
		
		if (arenaName == null || player == null) return false;
		
		if (!this.isPlayerInLobby(arenaName, player)) return false;
		
		List<String> playersInLobby = this.playersInLobby.get(arenaName.toLowerCase());
		
		playersInLobby.remove(player.getName().toLowerCase());
		
		this.playersInLobby.put(arenaName.toLowerCase(), playersInLobby);
		return true;
		
	}
	
	/** A method for removing the player from the lobby
	 * @param arenaName The name of the arena to remove the player from
	 * @param playerName The playername of who needs to be removed from the specified arena
	 * @return Will return true if the specified player was successfully removed from the arena's lobby
	 */
	public boolean removePlayerFromLobby(String arenaName, String playerName) {
		
		if (arenaName == null || playerName == null) return false;
		
		if (!this.isPlayerInLobby(arenaName, playerName)) return false;
		
		List<String> playersInLobby = this.playersInLobby.get(arenaName.toLowerCase());
		
		playersInLobby.remove(playerName.toLowerCase());
		
		this.playersInLobby.put(arenaName.toLowerCase(), playersInLobby);
		return true;
		
	}
	
	/** A simple method for checking if a players is in the specified arena's lobby
	 * @param arenaName The arena with the lobby in question
	 * @param player The player in question
	 * @return Will return true if the player is in the specified player's lobby
	 */
	public boolean isPlayerInLobby(String arenaName, Player player) {
		
		boolean doesContain = false;
		
		List<String> playersInLobby = this.playersInLobby.get(arenaName.toLowerCase());
		
		if (playersInLobby == null || playersInLobby.size() < 1) return false;
		
		for (String s : this.playersInLobby.get(arenaName)) {
			if (player.getName().equalsIgnoreCase(s)) {
				doesContain = true;
				break;
			}
		}
		
		return doesContain;
		
	}
	
	/** A simple method for checking if a players is in the specified arena's lobby
	 * @param arenaName The arena with the lobby in question
	 * @param playerName The name of the player in question
	 * @return Will return true if the player is in the specified player's lobby
	 */
	public boolean isPlayerInLobby(String arenaName, String playerName) {
		
		boolean doesContain = false;
		
		List<String> playersInLobby = this.playersInLobby.get(arenaName.toLowerCase());
		
		if (playersInLobby == null || playersInLobby.size() < 1) return false;
		
		for (String s : this.playersInLobby.get(arenaName)) {
			if (playerName.equalsIgnoreCase(s)) {
				doesContain = true;
				break;
			}
		}
		
		return doesContain;
		
	}
	
	/** Will return if the name of the arena that a player is in. Will return null if none
	 * @param p The player in question
	 * @return If the specified player is in an arena, it will return the name of that arena, if they are not in any arena, it will return null.
	 */
	public String getPlayerArenaName(Player p) {
		
		for (String s : SurvivalGames.getArenaData().listArenas()) {
			
			if (this.isPlayerInLobby(s, p.getName())) {
				return s;
			}
			
		}
		
		return null;
		
	}
	
	/** Will return if the name of the arena that a player is in. Will return null if none
	 * @param p The player in question
	 * @return If the specified player is in an arena, it will return the name of that arena, if they are not in any arena, it will return null.
	 */
	public String getPlayerArenaName(String p) {
		
		for (String s : SurvivalGames.getArenaData().listArenas()) {
			
			if (this.isPlayerInLobby(s, p)) {
				return s;
			}
			
		}
		
		return null;
		
	}
	
	/** A method for getting all the player's in the specified arena's lobby
	 * @param arenaName The arena to get all the players in the specified lobby
	 * @return A String list of all the players in the lobby
	 */
	public List<String> getPlayersInLobby(String arenaName) {
		
		return this.playersInLobby.get(arenaName.toLowerCase());
		
	}
	
	/** A method for determining of there are enough players to start a game from the lobby
	 * @param arenaName The name of the arena to check if it's ready to start the lobby countdown
	 * @return Will return true if there are enough players to start the match
	 */
	public boolean enoughPlayersToStart(String arenaName) {
		
		String newArenaName = arenaName.toLowerCase();
		
		if (!SurvivalGames.getArenaData().containsArena(newArenaName)) return false;
		
		if (SurvivalGames.getArenaData().getMinimumPayerCount(newArenaName) <= this.getAmountOfPlayersInLobby(newArenaName)) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** A simple method for getting how many players are currently in the lobby
	 * @param arenaName The name of the arena in question
	 * @return Will return a primitive int of how many players are in the lobby (0 if none obviously)
	 */
	public int getAmountOfPlayersInLobby(String arenaName) {
		
		String newArenaName = arenaName.toLowerCase();
		
		if (!SurvivalGames.getArenaData().containsArena(newArenaName)) return 0;
		
		List<String> playersInArena = this.getPlayersInLobby(newArenaName);
		
		if (playersInArena == null) {
			return 0;
		} else {
			return playersInArena.size();
		}
		
	}
	
	/** Clears out all the data for the lobby of the specified arena
	 * @param arenaName The arena name that the lobby will be cleared
	 */
	public void clearLobby(String arenaName) {
		
		this.playersInLobby.remove(arenaName.toLowerCase());
		this.lobbyTime.remove(arenaName.toLowerCase());
		
	}

	/** Sends a message to all the players in the specified arena's lobby
	 * @param arenaName The arena to send the message to the players
	 * @param message The message to send
	 */
	public void messagePlayersInLobby(String arenaName, String message) {
		
		for (String s : this.getPlayersInLobby(arenaName)) {
			
			Player player = Bukkit.getServer().getPlayer(s);
			
			if (player != null) {
				
				player.sendMessage(message);
				
			}
			
		}
		
	}
	
	/** Will return if the specified player is in any arena
	 * @param p The player in question
	 * @return If the specified player is in an lobby, it will return the name of that arena's lobby, if they are not in any lobby, it will return null.
	 */
	public String playerInLobby(Player p) {
		
		for (String s : SurvivalGames.getArenaData().listArenas()) {
			
			if (this.isPlayerInLobby(s, p)) {
				return s;
			}
			
		}
		
		return null;
		
	}
	
	/** A method used to tell if an arena cannot receive any more players. If either the player count or the maximum player count is zero, the method will always
	 * return false;
	 * @param arenaName The arena in question
	 * @return Will return true if the specified arena has a player count that is greater or equal to its maximum player count
	 */
	public boolean isFull(String arenaName) {
		
		int amountOfPlayersInLobby = this.getAmountOfPlayersInLobby(arenaName);
		int maximumPlayerCount = SurvivalGames.getArenaData().getMaximumPlayerCount(arenaName);
		
		if (amountOfPlayersInLobby == 0 || maximumPlayerCount == 0) return false;
		
		if (maximumPlayerCount <= amountOfPlayersInLobby) {
			return true;
		} else {
			return false;
		}
		
	}
	
}
