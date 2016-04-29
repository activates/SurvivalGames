package us.thetaco.survivalgames.clockwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.file.InventorySerialization;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.Values;

/** A class specifically for handing arena timings etc. 
 * @author activates
 *
 */
public class ArenaTimer {
	
	private SurvivalGames plugin;
	public ArenaTimer(SurvivalGames plugin) {
		this.plugin = plugin;
	}
	
	private Map<String, Integer> arenaTimerIDs = new HashMap<String, Integer>();
	private Map<String, Integer> arenaStartTimerIDs = new HashMap<String, Integer>();
	private Map<String, Integer> startTime = new HashMap<String, Integer>();
	private Map<String, Integer> gameTime = new HashMap<String, Integer>();
	
	/** The method to start the real "game." Here you define the arena name, the time till the game starts (like the ready set go etc), then how long the game will last
	 * @param arenaName The name of the arena to start.. If the lobby is still active this will not run
	 * @param startTime The time till the game starts
	 * @param gameTime The actually time the game lasts
	 */
	public void startArenaTimer(String arenaName, int startTime, int gameTime) {
		
		// checking to see if the lobby for the specified arena is still active:
		if (SurvivalGames.getLobbyHandler().activeLobby(arenaName)) return;
		
		// calling the start timer which will start the actual match when ready
		int startID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new ArenaStartTimer(arenaName, startTime, gameTime), 0L, 20L);
		
		// storing the timer ID, so that way we can cancel it
		arenaStartTimerIDs.put(arenaName.toLowerCase(), startID);
		
		// freeze all of the players, then unfreeze them once the game acutally starts
		SurvivalGames.getArenaData().freezeAll(arenaName);
		
	}
	
	private void startArenaGame(String arenaName, Integer gt) {
		
		// starting up the timer
		int startID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new ArenaGameTimer(arenaName, gt), 0L, 20L);
		
		// storing the ID
		this.arenaTimerIDs.put(arenaName.toLowerCase(), startID);
		
		// unfreeze all of the players!
		SurvivalGames.getArenaData().unfreezeAll(arenaName);
		
		// for now, just message players that the real game has started
		SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.GAME_STARTED.toString(), false);
		
	}
	
	/** A class just for handling the start of the game.. The timer that runs before the real game actually starts
	 * @author activates
	 *
	 */
	private class ArenaStartTimer implements Runnable {

		private int startTime;
		private String arenaName;
		private int gameTime;
		public ArenaStartTimer(String arenaName, Integer startTime, Integer gameTime) {
			
			if (startTime == null) {
				this.startTime = 1;
			} else {
				this.startTime = startTime;
			}
			
			this.arenaName = arenaName;
			this.gameTime = gameTime;
			
		}
		
		@Override
		public void run() {
			
			// stopping this method if the timer is no longer needed
			if (!startTimerRunning(this.arenaName)) {
				return;
			}
			
			if (!isArenaStartTimeSet(arenaName)) {
				setArenaStartTime(arenaName, startTime);
				
				// stopping the looping here, then start counting down next time it runs
				
				return;
			}
			
			int currentTime = getCurrentStartTime(arenaName);
			
			// check to see what the current loop interval is at.. and if it is at zero, start the match
			
			if (currentTime > 0) {
				
				// run this if the current time is not done  yet
				
				// for now, we'll just get the numbers to notify from the lobby.. because I'm too lazy to do it for this too
				List<Integer> numbersToNotify = SurvivalGames.getArenaData().getNumbersToNotify();
				
				// checking to see if the numbers to notify actually checks the number correctly! :D
				boolean notify = false;
				for (Integer i : numbersToNotify) {
					if (i != null && currentTime == i) notify = true;
				}
				
				if (notify) {
					
					// if we reach this point, we will notify all the players in the arena of what time it is
					
					for (String s : SurvivalGames.getArenaData().getPlayersInArena(arenaName.toLowerCase(), false)) {
						
						Player inArena = plugin.getServer().getPlayer(s);
						
						// we need to check that the player didn't log out (we will also check in a disconnect listener)
						if (inArena == null) {
							
							SurvivalGames.getArenaData().removePlayerFromArena(arenaName, s);
							
							// we also need to check that there are enough players...
							if (!SurvivalGames.getArenaData().areEnoughPlayersToStart(arenaName)) {
								// if we get to this point.. we know there aren't enough players to run the arena anymore
								// TODO: cancel the game here if not enough players are in to really start the game
								return;
							}
							
						} else {
							
							// if we get to this point, we know the player exists, so we can send them the message
							inArena.sendMessage(Language.GAME_STARTING_TIMER.toString(null, null, currentTime, null));
							
						}
						
					}
					
				}
				
				/*
				 * This is just the start timer... so there is no REAL reason a tick method is needed
				 */
				
			} else {
				
				// removing the timer, so it no longer runs
				cancelStartTimer(arenaName);
				
				// clearing the start timer value
				clearCurrentStartTime(arenaName);
				
				// run this if the match is ready to start 
				startArenaGame(arenaName, gameTime);
				
			}
			
			currentTime--;
			
			setCurrentStartTime(arenaName, currentTime);
			
		}
		
	}
	
	/** A private nested-class for managing the arena while it's running. It runs in 1 second intervals
	 * @author activates
	 *
	 */
	private class ArenaGameTimer implements Runnable {

		private String arenaName;
		private int gameTime;
		public ArenaGameTimer(String arenaName, Integer gameTime) {
			
			this.arenaName = arenaName;
			
			if (gameTime == null) {
				this.gameTime = 1;
			} else {
				this.gameTime = gameTime;
			}
			
		}
		
		@Override
		public void run() {
			
			if (!gameTimerRunning(arenaName)) {
				// if the game timer shouldn't be running.. then we need to stop this
				
				return;
			}
			
			// checking to see if the game time has been set.. if not.. set it! :D
			if (!isArenaGameTimeSet(arenaName)) {
				// setting the timer and stopping the loop here if it just started
				setArenaGameTime(arenaName, gameTime);
				return;
			}
			
			// if we get to this point, we know that the arena start time has been set
			int currentTime = getCurrentGameTime(arenaName);
			
			// now check to see if the arena is finished or not..
			// if it's not finished, there are a WHOLE lotta things we need to do!
			if (currentTime > 0) {
				
				/*
				 * 
				 * In this 'if' statement, all normal arena checks will be made here. Listeners
				 * will take care of most of the brunt, but simple things (like the scoreboard timer) will be handled here
				 * 
				 */
				
				/*
				 * Since this is the arena "tick" we will put everything in a seperate method named tick for
				 * organization's sake
				 */
				
				this.tick(currentTime);
				
			} else {
				
				SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.TIMER_ENDING.toString(arenaName, null, null, null), true);
				
				// If this runs, the game is fished.. so we need to close everything off
				closeGame(arenaName);
				return;
				
			}
			
			// decrementing the time and setting it
			currentTime--;
			
			setCurrentGameTime(arenaName, currentTime);
			
		}
		
		private Integer nextRadiusTick = null;
		
		/** A method that will make all necessary checks for every arena "tick" which runs
		 * approximately every second
		 * 
		 */
		private void tick(int currentTime) {
			
			// -- BEGIN playercount check --
			
			// here we are checking if there is only 1 player left. If there is only one player
			// left in the arena, we will close the game.
			
			int playerAmount = SurvivalGames.getArenaData().getAmountOfPlayerInArena(arenaName, false);
			
			if (playerAmount <= 1) {
				
				SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.TIMER_ENDING.toString(arenaName, null, null, null), true);
				
				// if this runs, close the game and stop the tick here
				closeGame(arenaName);
				return;
				
			}
			
			// -- END playercount check
			
			/*
			 * MORE SPACER TIME (I need to use the bathroom)
			 */
			
			// -- BEGIN scoreboard timer --
			
			ScoreboardManager manager = Bukkit.getScoreboardManager();
			Scoreboard board = manager.getNewScoreboard();
		    
			Objective objective = board.registerNewObjective("SurvivalGames", "dummy");
			objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			objective.setDisplayName("Time Left");
			
			// Setting the time
		    Score score = objective.getScore(ChatColor.GREEN + "Seconds:"); //Get a fake offline player
		    score.setScore(currentTime);
			
			for (String s : SurvivalGames.getArenaData().getPlayersInArena(arenaName, false)) {
				
				// printing the scoreboard to these players
				Player inArena = Bukkit.getPlayer(s);
				
				if (inArena != null) {
					inArena.setScoreboard(board);
				}
				
			}
			
			// -- END scoreboard timer --
			
			/*
			 * SPACER TIME hOI!
			 */
			
			// -- BEGIN border shrinking --
			
			// make sure borders are enabled
			if (Values.BORDER_ENABLED) {
								
				// check to see if the border has been set, and set it if it hasn't been
				if (!SurvivalGames.getArenaData().isBorderRadiusSet(arenaName)) {
					SurvivalGames.getArenaData().setBorderRadius(arenaName, SurvivalGames.getArenaData().getDefaultBorderRadius(arenaName));
				}
				
				// first we need to make sure that the time has been intialized
				if (nextRadiusTick == null) {
										
					// if it is, we'll set it to the default value
					nextRadiusTick = Values.BORDER_TICK_DELAY;
					
				} else {
					
					// if it has already been set, we'll decrement it here
					nextRadiusTick--;
										
				}
			
				// we'll check to see if it's at zero, so we can bring down the border size
				if (nextRadiusTick < 1) {
				
					// grab the current border size, and shrink it if possible
					double currentBorderSize = SurvivalGames.getArenaData().getBorderRadius(arenaName);
					
					if (currentBorderSize > 0) {
						
						// checking to see if the border floor has been reached
						if (currentBorderSize <= Values.BORDER_FLOOR){
							// do nothing if the border has reached the floor!
							// actually don't just do nothing, set the border to the floor
							currentBorderSize = Values.BORDER_FLOOR;
						} else {
							// decrement the border size by the set interval
							currentBorderSize = currentBorderSize - Values.BOREDER_DECREMENT_AMOUNT;
							
							// set number to 0 if it ends up being negative
							if (currentBorderSize < 0) {
								currentBorderSize = 0;
							}
						}
					}
					
					// set the next radius tick to null, so it will be reset
					nextRadiusTick = null;
					
					SurvivalGames.getArenaData().setBorderRadius(arenaName, currentBorderSize);
					
				}
				
			}
			
			// -- END border shrinking --
			
			/*
			 * HAPPY SPACER!!! (I wish I could do colors in my notes :T)
			 */
			
			// -- BEGIN center distance checking --
			
			// make sure borders are enabled
			if (Values.BORDER_ENABLED) {
			
				// retrieve and deformat the center location
				Location center = SurvivalGames.getArenaData().getCenterLocation(arenaName);
				
				// look through all players within the arena and check to see if they are outside the arena
				for (String s : SurvivalGames.getArenaData().getPlayersInArena(arenaName, false)) {
					
					// check and make sure the player is online & still in the arena
					Player inArena = Bukkit.getPlayer(s);
					
					// we should also make sure the player is in the same world as the center location
					if (inArena != null && center.getWorld().getName().equalsIgnoreCase(inArena.getWorld().getName())) {
												
						// Time to see if the player is outside the border
						double pDistanceFromCenter = SurvivalGames.getArenaData().distanceFromCenter(arenaName, inArena.getLocation()) ;
						double borderRadius = SurvivalGames.getArenaData().getBorderRadius(arenaName);
						
						if (pDistanceFromCenter > borderRadius) {
							
							// this will run if the player is outside the border
							
							// just send the player a message for debugging reasons
							inArena.sendMessage("You are outside of the border!");
							
							// TODO: punish player for exiting the barrier
							
						}
						
					}
					
				}
				
			}
			
			// -- END distance checking --
			
		}
		
	}
	
	/** A method for determining if an arena's game is actually started, yet. If not, chances are the timer section could be active
	 * @param arenaName The arena in question
	 * @return Will return true if the game is active and started
	 */
	public boolean gameStarted(String arenaName) {
		
		if (this.arenaTimerIDs.containsKey(arenaName.toLowerCase())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** A method used to check and see if the timer is active and running
	 * @param arenaName The arena in question
	 * @return Will return true if the timer is active, but the game has not been started
	 */
	public boolean startTimerRunning(String arenaName) {
		
		if (this.arenaStartTimerIDs.containsKey(arenaName.toLowerCase())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** A method used to check and see if the timer is active and running for the game
	 * @param arenaName The arena in question
	 * @return Will return true if the timer is active and that the game is running (in theory)
	 */
	public boolean gameTimerRunning(String arenaName) {
		
		if (this.arenaTimerIDs.containsKey(arenaName.toLowerCase())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** Used to cancel the start timer.. Either if the match has been cancelled, or if the match has been started and the timer is no longer needed
	 * @param arenaName The name of the arena to stop the start timer
	 */
	public void cancelStartTimer(String arenaName) {
		
		Integer timerID = this.arenaStartTimerIDs.get(arenaName.toLowerCase());
		
		if (timerID != null) Bukkit.getScheduler().cancelTask(timerID);
		
		this.arenaStartTimerIDs.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to cancel the game timer.. Either if the match has been cancelled, or if the match has been started and the timer is no longer needed
	 * @param arenaName The name of the arena to stop the start timer
	 */
	public void cancelGameTimer(String arenaName) {
		
		
		Integer timerID = this.arenaTimerIDs.get(arenaName.toLowerCase());
		
		if (timerID != null) Bukkit.getScheduler().cancelTask(timerID);
		
		this.arenaTimerIDs.remove(arenaName.toLowerCase());
		
	}
	
	/** Used for determining if the arena timer has been set. Set to private because it's not too useful anywhere else
	 * @param arenaName The name of the arena in question
	 */
	private boolean isArenaStartTimeSet(String arenaName) {
		
		if (this.startTime.containsKey(arenaName.toLowerCase())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** Used for determining if the arena timer has been set. Set to private because it's not too useful anywhere else
	 * @param arenaName The name of the arena in question
	 */
	private boolean isArenaGameTimeSet(String arenaName) {
		
		if (this.gameTime.containsKey(arenaName.toLowerCase())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** Used to set the arena start time for looping purposes..
	 * @param arenaName The name of the arena to set the start timer for
	 * @param startTime The desired start time
	 */
	private void setArenaStartTime(String arenaName, int startTime) {
		
		this.startTime.put(arenaName.toLowerCase(), startTime);
		
	}
	
	/** Used to set the arena game time for looping purposes..
	 * @param arenaName The name of the arena to set the start timer for
	 * @param startTime The desired game time
	 */
	private void setArenaGameTime(String arenaName, int gameTime) {
		
		this.gameTime.put(arenaName.toLowerCase(), gameTime);
		
	}
	
	/** Used to retrieve the current start time interval. It is done in this method to prevent null pointer exceptions
	 * @param arenaName The name of the arena to retrieve the current start time
	 * @return The current interval of the timer.. It will be zero if it is disabled
	 */
	public int getCurrentStartTime(String arenaName) {
		
		// used for getting the current time. Done this way because null might exist.. and we don't want that D:
		Integer toReturn = this.startTime.get(arenaName.toLowerCase());
		
		if (toReturn == null) toReturn = 0;
		
		return toReturn;
		
	}
	
	/** Used to retrieve the current game time interval. It is done in this method to prevent null pointer exceptions
	 * @param arenaName The name of the arena to retrieve the current game time
	 * @return The current interval of the timer. If it is zero, the game is not active
	 */
	public int getCurrentGameTime(String arenaName) {
		
		// used for getting the current time. Done this way because null might exist.. and we don't want that D:
		Integer toReturn = this.gameTime.get(arenaName.toLowerCase());
		
		if (toReturn == null) toReturn = 0;
		
		return toReturn;
		
	}
	
	/** Used to set the current interval on the loop. (not to be confused with setArenaStartTime)
	 * @param arenaName The arena in question
	 * @param startTime The new value for the loop time
	 */
	private void setCurrentStartTime(String arenaName, int startTime) {
		
		if (startTime < 1) {
			
			this.startTime.put(arenaName.toLowerCase(), 0);
			
		} else {
		
			this.startTime.put(arenaName.toLowerCase(), startTime);
		
		}
	}
	
	/** Used to set the current interval on the loop. (not to be confused with setArenaGameTime)
	 * @param arenaName The arena in question
	 * @param startTime The new value for the loop time
	 */
	private void setCurrentGameTime(String arenaName, int gameTime) {
		
		if (gameTime < 1) {
			
			this.gameTime.put(arenaName.toLowerCase(), 0);
			
		} else {
		
			this.gameTime.put(arenaName.toLowerCase(), gameTime);
		
		}
	}

	/** Used to clear the actually incrementing start timer, not the start timer ID
	 * @param arenaName The arena to stop the start timer in
	 */
	public void clearCurrentStartTime(String arenaName) {
		
		this.startTime.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to remove the scoreboard from all players in the arena. Only useful when closing the arena
	 * @param arenaName The name of the arena that all scoreboards need to removed in
	 */
	public void removeScoreboard(String arenaName) {
		
		for (String s : SurvivalGames.getArenaData().getPlayersInArena(arenaName.toLowerCase(), false)) {
			
			Player inArena = Bukkit.getPlayer(s);
			
			if (inArena != null) {
			
				inArena.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
			
			}
		
		}
	
	}
	
	/** Used to remove the scoreboard from a specific players. (Mainly used after they die)
	 * @param p The player that the scoreboard will be removed on
	 */
	public void removeScoreboard(Player p) {
		
		p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
	
	}
	
	/** A method simply for restoring the inventories to all the players within a particular arena
	 * @param arenaName The arena to restore all the player's inventories in
	 */
	public void restoreInventories(String arenaName) {
		
		Set<String> playersInLobby = SurvivalGames.getArenaData().getPlayersInArena(arenaName, false);
		
		if (playersInLobby == null) return;
		
		List<String> playerNames = new ArrayList<String>();
		playerNames.addAll(playersInLobby);
		
		InventorySerialization.unserializeInventories(playerNames);
		
	}
	
	/** Positively increments all reputations (by one) for any player in the specified arena. Used to reward players
	 * for completing the game! 
	 * @param arenaName
	 */
	private void assignReputation(String arenaName) {
		
		Set<String> playersInArena = SurvivalGames.getArenaData().getPlayersInArena(arenaName, false);
		
		for (String s : playersInArena) {
			
			Player inArena = Bukkit.getPlayer(s);
			
			if (inArena != null) {
				SurvivalGames.getArenaData().addReputation(inArena);
				inArena.sendMessage(Language.REP_INCREASED_GAME_COMPLETED.toString(arenaName, null, null, null));
			}
			
		}
		
	}
	
	/** A method solely for closing the game. It is in charge of teleporting players to a specified end-zone
	 * @param arenaName The arena name that should be closed
	 */
	public void closeGame(String arenaName) {
		
		// first, cancel the game, so that way the looping stops
		this.cancelStartTimer(arenaName);
		this.cancelGameTimer(arenaName);
		
		// then set the arena as inactive
		SurvivalGames.getArenaData().movePlayersToLobby(arenaName);
		SurvivalGames.getArenaData().moveSpectatorsToLobby(arenaName);
		SurvivalGames.getArenaData().gamemodeSpectators(arenaName, false);
		
		// get rid of the scoreboards!
		this.removeScoreboard(arenaName);
		
		this.restoreInventories(arenaName);
		
		// pass out reputation!
		this.assignReputation(arenaName);
		
		SurvivalGames.getArenaData().removeBorderRadius(arenaName);
		SurvivalGames.getArenaData().clearArena(arenaName);
		SurvivalGames.getArenaData().clearSpectators(arenaName);
		
		// clearing the arena time
		this.gameTime.remove(arenaName.toLowerCase());
		this.startTime.remove(arenaName.toLowerCase());
		
		SurvivalGames.getArenaData().setArenaActive(arenaName, false);
		
	}
	
}
