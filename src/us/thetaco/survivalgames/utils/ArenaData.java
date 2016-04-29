package us.thetaco.survivalgames.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.file.ArenaFile;
import us.thetaco.survivalgames.file.InventorySerialization;
import us.thetaco.survivalgames.utils.SimpleLogger.Condition;

/** A class simply for holding the data on the arenas
 * @author activates
 *
 */
public class ArenaData {

	private List<Integer> numbersToNotify = new ArrayList<Integer>();
	private List<String> activeArenas = new ArrayList<String>();
	private List<String> stuckPlayers = new ArrayList<String>();
	private List<String> loadedArenas = new ArrayList<String>();
	private Map<String, String> arenaLobbys = new HashMap<String, String>();
	private Map<String, List<String>> spawnPoints = new HashMap<String, List<String>>();
	private Map<String, Map<String, Boolean>> playersInArena = new HashMap<String, Map<String, Boolean>>();
	private Map<String, Integer> minimumPlayerCounts = new HashMap<String, Integer>();
	private Map<String, Integer> lobbyTime = new HashMap<String, Integer>();
	private Map<String, Integer> arenaTime = new HashMap<String, Integer>();
	private Map<String, List<String>> spectators = new HashMap<String, List<String>>();
	private Map<String, Integer> minimumReps = new HashMap<String, Integer>();
	private Map<String, String> centerLocation = new HashMap<String, String>();
	private Map<String, Integer> defaultBorderRadius = new HashMap<String, Integer>();
	private Map<String, Double> borderRadius = new HashMap<String, Double>();
	
	/** A method for manually setting the list of the loaded arenas
	 * @param arenas A String list of the new loaded arenas
	 */
	public void setArenas(List<String> arenas) {
		
		loadedArenas = arenas;
		
	}
	
	/** Returns a string list of all the loaded arenas
	 * @return A string list containing all the loaded arenas
	 */
	public List<String> listArenas() {
		
		return loadedArenas;
		
	}
	
	/** Used for adding a new loaded-arena to the list
	 * @param arenaName The name of the arena to be added
	 * @param spawnPoints The spawnpoints for the specified arena
	 */
	public void addArena(String arenaName, List<String> spawnPoints) {
		
		String newArenaName = arenaName.toLowerCase();
		
		if (!this.containsArena(newArenaName)) {
			loadedArenas.add(newArenaName);
			this.spawnPoints.put(newArenaName, spawnPoints);
		}
		
	}
	
	/** Removes the arena from the list
	 * @param arenaName The name of the arena that is to be removed
	 * @return Will return false if the arena existed and was removed
	 */
	public boolean removeArena(String arenaName) {
		
		String newArenaName = arenaName.toLowerCase();
		
		List<String> cloneList = loadedArenas;
		boolean foundMatch = false;
		
		for (String s : loadedArenas) {
			if (s.equalsIgnoreCase(newArenaName)) {
				cloneList.remove(s);
				foundMatch = true;
				break;
			}
		}
		
		if (foundMatch) {
			this.loadedArenas = cloneList;
			this.spawnPoints.remove(newArenaName);
			return true;
		} else {
			return false;
		}
		
	}
	
	/** An method similar to equalIgnoreCase, but for a String list
	 * @param name The name of the method that could be inside
	 * @return Will return true if the arenaList contains the specified name
	 */
	public boolean containsArena(String name) {
		
		for (String s : loadedArenas) {
			if (s.equalsIgnoreCase(name)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	/** Used for getting an arena's defined spawnpoints
	 * @param arenaName The name of the specified arena
	 * @return The spawn points of the specified arena. Will return null if there aren't any (which would be bad)
	 */
	public List<String> getSpawnPoints(String arenaName) {
		
		String newArenaName = arenaName.toLowerCase();
		
		return this.spawnPoints.get(newArenaName);
		
	}
	
	/** This method's job is to add a player to the specified arena. It will not add a player if they
	 * are already in the list. This will also, by default, set the player to ALIVE
	 * @param arenaName The name of the arena to add the specified player
	 * @param player The player object who will be added to the arena
	 */
	public void addPlayerToArena(String arenaName, Player player) {
		
		String newArenaName = arenaName.toLowerCase();
		
		// getting a set of all the player's names
		Map<String, Boolean> playersInArenaMap = this.playersInArena.get(newArenaName);
		
		// checking to make sure that there are any entries in the list
		if (playersInArenaMap == null) {
			
			// creating the new hashmap to prevent null pointer exceptions
			playersInArenaMap = new HashMap<String, Boolean>();
			playersInArenaMap.put(player.getName().toLowerCase(), false);
			
			// storing the new hashmap in the list for later use
			this.playersInArena.put(newArenaName, playersInArenaMap);
			
		} else {
			
			if (!this.arenaContainsPlayer(newArenaName, player, true)) {
				
				// since we use hashmaps, now, This will be a bit more complicated
				// we just need to put the new information in, then 'commit' it to the main list
				
				playersInArenaMap.put(player.getName().toLowerCase(), false);
				this.playersInArena.put(newArenaName, playersInArenaMap);
				
			}
			
		}
		
		
	}
	
	/** A method mainly for onEnable to load the arenas and their spawn points
	 * @param arenaName The arena to set the spawn points for
	 * @param spawnPoints A string list contianing the FORMATTED spawn points.. You can format the string with the format method in the ArenaFile class:
	 * ArenaFile#formatString
	 */
	public void setSpawnPoints(String arenaName, List<String> spawnPoints) {
		
		this.spawnPoints.put(arenaName.toLowerCase(), spawnPoints);
		
	}
	
	/** This method's job is to add a player to the specified arena. It will not add a player if they
	 * are already in the list
	 * @param arenaName The name of the arena to add the specified player
	 * @param player The player object who will be added to the arena
	 */
	public void addPlayerToArena(String arenaName, String player) {
		
		String newArenaName = arenaName.toLowerCase();
		
		// getting a set of all the player's names
		Map<String, Boolean> playersInArenaMap = this.playersInArena.get(newArenaName);
		
		// checking to make sure that there are any entries in the list
		if (playersInArenaMap == null) {
			
			// creating the new hashmap to prevent null pointer exceptions
			playersInArenaMap = new HashMap<String, Boolean>();
			playersInArenaMap.put(player.toLowerCase(), false);
			
			// storing the new hashmap in the list for later use
			this.playersInArena.put(newArenaName, playersInArenaMap);
			
		} else {
			
			if (!this.arenaContainsPlayer(newArenaName, player)) {
				
				// since we use hashmaps, now, This will be a bit more complicated
				// we just need to put the new information in, then 'commit' it to the main list
				
				playersInArenaMap.put(player.toLowerCase(), false);
				this.playersInArena.put(newArenaName, playersInArenaMap);
				
			}
			
		}	
		
		
	}
	
	/** Removes the specified player from the specified arena
	 * @param arenaName The arena to check for the player in
	 * @param player The player in question
	 * @return Will return true if the player was in the arena and removed
	 */
	public boolean removePlayerFromArena(String arenaName, Player player) {
		
		String newArenaName = arenaName.toLowerCase();
		
		if (!this.arenaContainsPlayer(newArenaName, player, true)) return false;

		Map<String, Boolean> newMap = this.playersInArena.get(newArenaName);
		
		if (newMap != null) {
			newMap.remove(player.getName().toLowerCase());
			
			this.playersInArena.put(newArenaName, newMap);
			
			return true;
		} else {
			return false;
		}
		
	}
	
	/** Removes the specified player from the specified arena
	 * @param arenaName The arena to check for the player in
	 * @param player The player in question
	 * @return Will return true if the player was in the arena and removed
	 */
	public boolean removePlayerFromArena(String arenaName, String player) {
		
		String newArenaName = arenaName.toLowerCase();
		
		if (!this.arenaContainsPlayer(newArenaName, player)) return false;

		Map<String, Boolean> newMap = this.playersInArena.get(newArenaName);
		
		if (newMap != null) {
			newMap.remove(player.toLowerCase());
			
			this.playersInArena.put(newArenaName, newMap);
			
			return true;
		} else {
			return false;
		}
		
	}
	
	/** Checks if the specified arena contains the specified player
	 * @param arenaName The name of the arena to check in for the player
	 * @param player The player in question
	 * @param showDead If set to true, it will count players who are dead in the arena as part of the arena. If false
	 * it will not count them as in the arena, even if they are still in the Map
	 * @return Will return true if the specified arena contains the specified player
	 */
	public boolean arenaContainsPlayer(String arenaName, Player player, boolean showDead) {
		
		String newArenaName = arenaName.toLowerCase();
		
		Map<String, Boolean> playersInArena = this.playersInArena.get(newArenaName);
		boolean doesContain = false;
		
		if (playersInArena == null) return false;
		
		for (String s : playersInArena.keySet()) {
			
			if (s.equalsIgnoreCase(player.getName())) {
				
				Boolean isDead = playersInArena.get(s);
				
				if (isDead != null && isDead == true) {
					
					if (showDead) {
						
						doesContain = true;
						
					}
					
				} else {
					
					doesContain = true;
					
				}
				
				break;
				
			}
			
		}
		
		return doesContain;
		
	}
	
	/** Checks if the specified arena contains the specified player
	 * @param arenaName The name of the arena to check in for the player
	 * @param player The player in question
	 * @return Will return true if the specifed arena contains the specified player
	 */
	public boolean arenaContainsPlayer(String arenaName, String player) {
		
		String newArenaName = arenaName.toLowerCase();
		
		Map<String, Boolean> playersInArena = this.playersInArena.get(newArenaName);
		boolean doesContain = false;
		
		if (playersInArena == null) return false;
		
		for (String s : playersInArena.keySet()) {
			
			if (s.equalsIgnoreCase(player)) {
				doesContain = true;
				break;
			}
			
		}
		
		return doesContain;
		
	}
	
	/** Returns the list of all the players in the specified arena.
	 * @param arenaName The arena in question
	 * @param includeDead If set to true, will include all dead players in the list
	 * @return Returns a String list of all the players names in the arena. Will return an empty list
	 */
	public Set<String> getPlayersInArena(String arenaName, boolean includeDead) {
		
		String newArenaName = arenaName.toLowerCase();
		
		Map<String, Boolean> inArena = this.playersInArena.get(newArenaName);
		
		if (inArena == null) return new HashSet<String>();
		
		Set<String> toReturn = new HashSet<String>();
		
		for (String s : inArena.keySet()) {
			
			if (inArena.get(s) != null && inArena.get(s) == false) {
				
				toReturn.add(s);
				
			} else {
				
				if (includeDead) {
					toReturn.add(s);
				}
				
			}
			
		}
		
		if (toReturn.size() < 1) {
			return new HashSet<String>();
		} else {
			return toReturn;
		}
		
	}
	
	/** Used to determine how many players are in the specified arena
	 * @param arenaName The name of the arena to count how many players are in it
	 * @param countDead If set to true: will use dead players in the player count
	 * @return The amount of players in the arena
	 */
	public int getAmountOfPlayerInArena(String arenaName, boolean countDead) {
		
		String newArenaName = arenaName.toLowerCase();
		
		if (!this.containsArena(newArenaName)) return 0;
		
		Set<String> playersInArena = this.getPlayersInArena(newArenaName, countDead);
		
		if (playersInArena == null) {
			return 0;
		} else {
			return playersInArena.size();
		}
		
	}
	
	/** A method for setting the minimum player count of an arena
	 * @param arenaName The arena to set the amount for
	 * @param minimumPlayerCount The new minimum player count for the specified arena
	 */
	public void setMinimumPlayerCount(String arenaName, int minimumPlayerCount) {
		
		String newArenaName = arenaName.toLowerCase();
		
		this.minimumPlayerCounts.put(newArenaName, minimumPlayerCount);
		
	}
	
	/** Returns the minimum amount of players an arena must have in order for it to start
	 * @param arenaName The name of the arena for which you need to know its minimum player count
	 * @return An integer of how many players the arena needs before it can start
	 */
	public int getMinimumPayerCount(String arenaName) {
		
		String newArenaName = arenaName.toLowerCase();
		
		if (!this.containsArena(newArenaName)) return 1;
		
		Integer minimumPlayerCount = this.minimumPlayerCounts.get(newArenaName);
		
		if (minimumPlayerCount == null) {
			return 1;
		} else {
			return minimumPlayerCount;
		}
		
	}
	
	/** A quick method to tell if an arena has enough players to start
	 * @param arenaName The name of the arena you need to know about
	 * @return Will return true if the arena has enough players to start
	 */
	public boolean areEnoughPlayersToStart(String arenaName) {
		
		String newArenaName = arenaName.toLowerCase();
		
		if (!this.containsArena(newArenaName)) return false;
		
		if (this.getMinimumPayerCount(newArenaName) <= this.getAmountOfPlayerInArena(newArenaName, false)) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** A method for setting what number of seconds a player will be notified at when it reaches that point
	 * @param toNotify A list of integers to notify the player at
	 */
	public void setNumbersToNotify(List<Integer> toNotify) {
		
		List<Integer> mockList = new ArrayList<Integer>();
		
		if (toNotify == null || toNotify.size() > 1) {
			
			// a kind of failsafe.. just incase no numbers were specified
			mockList.add(60);
			mockList.add(30);
			mockList.add(10);
			mockList.add(9);
			mockList.add(8);
			mockList.add(7);
			mockList.add(6);
			mockList.add(5);
			mockList.add(4);
			mockList.add(3);
			mockList.add(2);
			mockList.add(1);
			
			this.numbersToNotify = mockList;
			
		} else {
			
			this.numbersToNotify = toNotify;
			
		}
		
	}
	
	/** Get the numbers that the player will be notified at when it reaches that specific time
	 * @return A list of integers to notify the players of the time
	 */
	public List<Integer> getNumbersToNotify() {
		
		// if (this.numbersToNotify == null || this.numbersToNotify.size() > 1) {
			
			List<Integer> newList = new ArrayList<Integer>();
			
			// a kind of failsafe.. just incase no numbers were specified
			newList.add(60);
			newList.add(30);
			newList.add(10);
			newList.add(9);
			newList.add(8);
			newList.add(7);
			newList.add(6);
			newList.add(5);
			newList.add(4);
			newList.add(3);
			newList.add(2);
			newList.add(1);
			
			this.numbersToNotify = newList;
			
		// }
		
		return numbersToNotify;
		
	}
	
	/** A method for setting if the specified arena is active or not
	 * @param arenaName The arena you wish to change the 'active' status on
	 * @param isActive The new status of the specified arena
	 */
	public void setArenaActive(String arenaName, boolean isActive) {
		
		if (!isActive) {
			
			this.activeArenas.remove(arenaName.toLowerCase());
			
		} else {
			
			if (!this.isArenaActive(arenaName)) {
				
				this.activeArenas.add(arenaName.toLowerCase());
				
			}
			
		}
		
	}
	
	/** A method for determining if an arena is active or not
	 * @param arenaName The arena you wish to check for
	 * @return Will return true if the arena is active...
	 */
	public boolean isArenaActive(String arenaName) {
		
		boolean doesContain = false;
		
		for (String s : this.activeArenas) {
			
			if (s.equalsIgnoreCase(arenaName)) {
				doesContain = true;
				break;
			}
			
		}
		
		return doesContain;
		
	}
	
	/** Will return if the specified player is in any arena. It will not return true if the player is in the arena, but is "dead."
	 * @param p The player in question
	 * @return If the specified player is in an arena, it will return the name of that arena, if they are not in any arena, it will return null.
	 */
	public String playerInArena(Player p, boolean includeDead) {
		
		for (String s : this.activeArenas) {
			
			if (this.arenaContainsPlayer(s, p, includeDead)) {
				return s;
			}
			
		}
		
		return null;
		
	}
	
	/** Will move all players in the arena to the lobby. It will also remove any players who are no longer online
	 * @param arenaName The name of the arena
	 */
	public void movePlayersToLobby(String arenaName) {
		
		Location arenaLobby = this.getArenaLobby(arenaName);
		
		if (arenaLobby == null) {
			SurvivalGames.logger().logMessage("The players in arena '" + arenaName + "' could not be teleported to the lobby because the lobby location is null!", Condition.ERROR);
			return;
		}
		
		for (String s : this.getPlayersInArena(arenaName, false)) {
			
			Player inArena = Bukkit.getPlayer(s);
			
			if (s != null) {
				
				inArena.teleport(arenaLobby);
				
			}
			
		}
		
	}
	
	/** Used to move a specific player to the arena's lobby
	 * @param arenaName The name of the arena to send the player to its lobby
	 * @param player The player to send to the specified lobby
	 */
	public void movePlayerToLobby(String arenaName, Player player) {
		
		Location arenaLobby = this.getArenaLobby(arenaName);
		
		if (arenaLobby == null) {
			SurvivalGames.logger().logMessage("The players in arena '" + arenaName + "' could not be teleported to the lobby because the lobby location is null!");
			return;
		}
		
		player.teleport(arenaLobby);
		
	}
	
	/** Used for cleaning out arenas because either they were cancelled or the game has ended. It will remove all players and set the arena as inactive.
	 * This should only be ran once all players have been teleported to the lobby because after this is ran all data will be been removed.
	 * @param arenaName The name of the arena to close
	 */
	public void clearArena(String arenaName) {
		
		SurvivalGames.getArenaData().removeBorderRadius(arenaName);
		this.playersInArena.remove(arenaName.toLowerCase());
		this.setArenaActive(arenaName, false);
		
	}
	
	/** Sends a message to all the players in the specified arena
	 * @param arenaName The arena to send the message to the players
	 * @param message The message to send
	 * @param messageDead Whether or not to message the players who are considered "dead"
	 */
	public void messagePlayersInArena(String arenaName, String message, boolean messageDead) {
		
		Map<String, Boolean> playerMap = this.playersInArena.get(arenaName.toLowerCase());
		
		if (playerMap == null) return;
		
		if (messageDead) {
			
			for (String s : playerMap.keySet()) {
				
				Player inArena = Bukkit.getPlayer(s);
			
				if (inArena != null) inArena.sendMessage(message);
				
			}
			
		} else {
			
			for (String s : playerMap.keySet()) {
				
				Boolean isDead = playerMap.get(s);
				
				if (isDead != null && isDead == false) {
				
					Player inArena = Bukkit.getPlayer(s);
				
					if (inArena != null) inArena.sendMessage(message);
					
				}
				
			}
			
		}
		
		
	}
	
	/** A method to determine if a player has been killed or not in an arena.
	 * @param arenaName The name of the arena to check if the player is dead in
	 * @param player The player in question
	 * @return Will return true if the player is alive. If the player is not in the map, it will return false.
	 */
	public boolean isPlayerDead(String arenaName, Player player) {
		
		Map<String, Boolean> playerMap = this.playersInArena.get(arenaName.toLowerCase());
		
		if (playerMap == null) return false;
		
		Boolean isPlayerDead = playerMap.get(player.getName().toLowerCase());
		
		if (isPlayerDead == null) return false;
		
		// if everything checks out, return the value stored in the map
		return isPlayerDead;
		
	}
	
	/** A method to determine if a player has been killed or not in an arena.
	 * @param arenaName The name of the arena to check if the player is dead in
	 * @param player The player in question
	 * @return Will return true if the player is alive. If the player is not in the map, it will return false.
	 */
	public boolean isPlayerDead(String arenaName, String player) {
		
		Map<String, Boolean> playerMap = this.playersInArena.get(arenaName.toLowerCase());
		
		if (playerMap == null) return false;
		
		Boolean isPlayerDead = playerMap.get(player.toLowerCase());
		
		if (isPlayerDead == null) return false;
		
		// if everything checks out, return the value stored in the map
		return isPlayerDead;
		
	}
	
	/** Used to set if the player is alive or not.
	 * @param arenaName The arena to set the player alive or not (will not change anything if the player is not in the arena)
	 * @param player The player to change the status on
	 */
	public void setPlayerDead(String arenaName, Player player, boolean isDead) {
		
		Map<String, Boolean> playerMap = this.playersInArena.get(arenaName.toLowerCase());
		
		// checking if the map has been initialized.. if it hasn't, do that!
		if (playerMap == null) {
			playerMap = new HashMap<String, Boolean>();
		}
		
		// force the new value upon the player. Even if it's the same, it won't hurt to set it
		// again
		playerMap.put(player.getName().toLowerCase(), isDead);
		this.playersInArena.put(arenaName.toLowerCase(), playerMap);
		
	}
	
	/** Used to set if the player is alive or not.
	 * @param arenaName The arena to set the player alive or not (will not change anything if the player is not in the arena)
	 * @param player The player to change the status on
	 */
	public void setPlayerDead(String arenaName, String player, boolean isDead) {
		
		Map<String, Boolean> playerMap = this.playersInArena.get(arenaName.toLowerCase());
		
		if (playerMap == null) return;
		
		Boolean isPlayerAlive = playerMap.get(player.toLowerCase());
		
		if (isPlayerAlive == null) return;
		
		// if everything checks out, update the value in the map
		playerMap.put(player.toLowerCase(), isDead);
		this.playersInArena.put(arenaName.toLowerCase(), playerMap);
		
	}
	
	/** A method used to get the maximum allowed players in an arena
	 * @param arenaName The arena name in question
	 * @return The integer amount of how many players can be in the arena at one time. Will return 0 if the
	 * arena is... Unhappy
	 */
	public int getMaximumPlayerCount(String arenaName) {
		
		List<String> spawnPoints = this.getSpawnPoints(arenaName);
		
		if (spawnPoints == null) return 0;
		
		return spawnPoints.size();
		
	}
	
	
	/** Moves all valid players in the arena to a spawn point. If all spawn points are taken, reuse another one.
	 * @param arenaName The name of the arena to send all players to spawn in
	 */
	public void distributePlayersToSpawns(String arenaName) {
		
		String newArenaName = arenaName.toLowerCase();
		
		Set<String> playersInArena = this.getPlayersInArena(newArenaName, false);
		List<String> playerList = new ArrayList<String>();
		playerList.addAll(playersInArena);
		List<String> spawnPoints = this.getSpawnPoints(newArenaName);
		
		if (playersInArena == null || playersInArena.size() < 1) return;
		
		int i = 0;
		
		while (playerList.size() > 0) {
			
			int spawnPointSize = spawnPoints.size();
			
			if (i+1 > spawnPointSize) i = 0;
			
			Player inArena = Bukkit.getPlayer(playerList.get(0));
			
			if (inArena != null) {
				
				Location teleportLocation = ArenaFile.deformatString(this.getSpawnPoints(newArenaName).get(i));
				
				if (teleportLocation == null) {
					inArena.sendMessage(ChatColor.RED + "There was an error while trying to teleport you to a spawn location. Reason: Teleport location NULL");
				} else {
					inArena.teleport(teleportLocation);
					// "clean" the player
					this.cleanPlayer(inArena);
				}
				
			}
			
			playerList.remove(0);
			i++;
			
		}
		
	}
	
	/** Freezes the inputted player.. No matter what. Be sure to unfreeze them or else they will never escape!
	 * @param player The player to freeze
	 */
	public void addFrozenPlayer(Player player) {
		
		if (this.isPlayerFrozen(player)) return;
		
		this.stuckPlayers.add(player.getName().toLowerCase());
		
	}
	
	/** Freezes the inputted player.. No matter what. Be sure to unfreeze them or else they will never escape!
	 * @param player The player to freeze
	 */
	public void addFrozenPlayer(String player) {
		
		if (this.isPlayerFrozen(player)) return;
		
		this.stuckPlayers.add(player.toLowerCase());
		
	}
	
	/** Used to un-freeze players
	 * @param player The player to unfreeze
	 * @return Will return true if the player specified was sucessfully unfrozen
	 */
	public boolean removeFrozenPlayer(Player player) {
		
		if (!this.isPlayerFrozen(player)) return false;
		
		this.stuckPlayers.remove(player.getName().toLowerCase());
		return true;
		
	}
	
	/** Used to un-freeze players
	 * @param player The player to unfreeze
	 * @return Will return true if the player specified was sucessfully unfrozen
	 */
	public boolean removeFrozenPlayer(String player) {
		
		if (!this.isPlayerFrozen(player)) return false;
		
		this.stuckPlayers.remove(player.toLowerCase());
		return true;
		
	}
	
	/** Used to un-freeze all the players in a particular arena!
	 * @param arenaName The arena to un-freeze all the players within
	 */
	public void unfreezeAll(String arenaName) {
		
		Set<String> playerSet = this.getPlayersInArena(arenaName, true);
		
		if (playerSet == null || playerSet.size() < 1) return;
		
		for (String s : playerSet) {
			
			this.removeFrozenPlayer(s);
			
		}
		
	}
	
	/** Used to freeze all the players in a particular arena!
	 * @param arenaName The arena to un-freeze all the players within
	 */
	public void freezeAll(String arenaName) {
		
		Set<String> playerSet = this.getPlayersInArena(arenaName, true);
		
		if (playerSet == null || playerSet.size() < 1) return;
		
		for (String s : playerSet) {
			
			this.addFrozenPlayer(s);
			
		}
		
	}
	
	/** Used to determine if a player is frozen.. If they are, unfreeze them
	 * @param player The player in question
	 * @return Will return true if the player is set to be frozen
	 */
	public boolean isPlayerFrozen(Player player) {
		
		boolean doesContain = false;
		
		for (String s : this.stuckPlayers) {
			
			if (s.equalsIgnoreCase(player.getName())) {
				doesContain = true;
				break;
			}
			
		}
		
		return doesContain;
		
	}
	
	/** Used to determine if a player is frozen.. If they are, unfreeze them
	 * @param player The player in question
	 * @return Will return true if the player is set to be frozen
	 */
	public boolean isPlayerFrozen(String player) {
		
		boolean doesContain = false;
		
		for (String s : this.stuckPlayers) {
			
			if (s.equalsIgnoreCase(player)) {
				doesContain = true;
				break;
			}
			
		}
		
		return doesContain;
		
	}
	
	/** used to 'clean' players/prepare them for a match
	 * @param player The player to be 'cleaned'
	 */
	public void cleanPlayer(Player player) {
		
		// setting the player's gamemode and clearing their inventory
		player.setGameMode(GameMode.SURVIVAL);
		
		PlayerInventory inv = player.getInventory();
		
		new InventorySerialization(player.getUniqueId().toString()).serializeInventory(inv);
		
		inv.setArmorContents(new ItemStack[4]);
		
		inv.clear();
		
		player.setHealthScale(20);
		player.setHealth(20);
		player.setFoodLevel(20);
		
		// removing potion effects
		for (PotionEffect p : player.getActivePotionEffects()) {
			
			player.removePotionEffect(p.getType());
			
		}
		
	}
	
	/** Used to set the location of a lobby
	 * @param arenaName The name of the arena to set the lobby location for
	 * @param loc The location of the Lobby (In Bukkit's format)
	 */
	public void setArenaLobby(String arenaName, Location loc) {
		
		if (loc == null) return;
		
		String preparedString = loc.getWorld() + "//" + loc.getBlockX() + "//" + loc.getBlockY() + "//" + loc.getBlockZ() + "//" + loc.getPitch() + "//" + loc.getYaw();
		
		this.arenaLobbys.put(arenaName.toLowerCase(), preparedString);
		
	}
	
	/** Used to set the location of a lobby
	 * @param arenaName The name of the arena to set the lobby location for
	 * @param loc The location of the Lobby (SurvivalGames format) Use {@link ArenaFile#formatString(World, double, double, double, float, float)}
	 */
	public void setArenaLobby(String arenaName, String formattedLocation) {
		
		if (formattedLocation == null) return;
		
		if (formattedLocation.split("//").length < 6) return;
		
		this.arenaLobbys.put(arenaName.toLowerCase(), formattedLocation);
		
	}
	
	/** Used to get the stored location of an arena's lobby
	 * @param arenaName The name of the arena to get the lobby for
	 * @return Will return the location (in Bukkit's form). If it is not stored, it will return null
	 */
	public Location getArenaLobby(String arenaName) {
		
		String preparedString = this.arenaLobbys.get(arenaName.toLowerCase());
				
		if (preparedString == null) return null;
		
		String[] splitString = preparedString.split("//");
		
		if (splitString.length < 6) {
			return null;
		}
		
		World world;
		double x = 0;
		double y = 0;
		double z = 0;
		float pitch = 0;
		float yaw = 0;
		
		try {
			
			world = Bukkit.getWorld(splitString[0]);
			x = Double.parseDouble(splitString[1]);
			y = Double.parseDouble(splitString[2]);
			z = Double.parseDouble(splitString[3]);
			yaw = Float.parseFloat(splitString[4]);
			pitch = Float.parseFloat(splitString[5]);
			
			if (world == null) throw new Exception();
			
		} catch (Exception e) {
			
			return null;
			
		}
		
		Location loc = new Location(world, x, y, z, yaw, pitch);
		
		return loc;
		
	}
	
	/** Used to restore an individuals inventory
	 * @param p The specified player who's inventory needs to be returned
	 */
	public void restoreInventory(Player p) {
		
		List<String> dudList = new ArrayList<String>();
		dudList.add(p.getName().toLowerCase());
		
		InventorySerialization.unserializeInventories(dudList);
		
	}
	
	/** Used to set the amount of time a lobby is held before it starts
	 * @param arenaName The name of the arena to set the time for
	 * @param seconds The total amount of wait time in seconds
	 */
	public void setLobbyTime(String arenaName, int seconds) {
		
		if (!this.containsArena(arenaName)) return;
		
		this.lobbyTime.put(arenaName.toLowerCase(), seconds);
		
	}
	
	/** Used to get the amount of time a lobby is held before it starts
	 * @param arenaName The name of the arena to get the time for
	 * @return The amount of time in seconds that a lobby is held for
	 */
	public int getLobbyTime(String arenaName) {
		
		Integer toReturn = this.lobbyTime.get(arenaName.toLowerCase());
		
		if (toReturn == null) {
			
			return 1;
			
		} else {
			
			return toReturn;
			
		}
		
	}
	
	/** Used to set the arena game time
	 * @param arenaName The name of the arena to set the game time for
	 * @param seconds The amount of time in seconds that an arena game is held
	 */
	public void setArenaTime(String arenaName, int seconds) {
		
		if (!this.containsArena(arenaName)) return;
		
		this.arenaTime.put(arenaName.toLowerCase(), seconds);
		
	}
	
	/** Used to get the arena game time
	 * @param arenaName The name of the arena
	 * @return Will return the time an arena is set for in seconds
	 */
	public int getArenaTime(String arenaName) {
		
		Integer toReturn = this.arenaTime.get(arenaName.toLowerCase());
		
		if (toReturn == null) {
			
			return 1;
			
		} else {
			
			return toReturn;
			
		}
		
	}

	/** Used to add a spectator to an arena
	 * @param arenaName The arena to add the player in
	 * @param player The spectator to be added
	 */
	public void addSpectator(String arenaName, Player player) {
		
		if (this.isSpectating(player) != null) return;
		
		List<String> dudList = this.spectators.get(arenaName.toLowerCase());
		
		if (dudList == null) {
			
			dudList = new ArrayList<String>();
			
			dudList.add(player.getName().toLowerCase());
			
		} else {
			
			dudList.add(player.getName().toLowerCase());
			
		}
		
		this.spectators.put(arenaName.toLowerCase(), dudList);
		
	}
	
	/** Used to remove a spectator from a game
	 * @param arenaName The arena to remove the spectator from
	 * @param player The player to remove as a spectator
	 * @return Will return true if the player was found and removed
	 */
	public boolean removeSpectator(String arenaName, Player player) {
		
		if (this.isSpectating(player) == null) return false;
		
		List<String> spectatingPlayers = this.spectators.get(arenaName.toLowerCase());
		
		if (spectatingPlayers == null) {
			return false;
		}
		
		return spectatingPlayers.remove(player.getName().toLowerCase());
		
	}
	
	/** Used to check if a player is spectating or not
	 * @param player The player to check
	 * @return Returns the name of the arena that the player is spectating in. Will return null
	 * if the player is not spectating
	 */
	public String isSpectating(Player player) {
		
		String pName = player.getName().toLowerCase();
		
		for (String s : this.listArenas()) {
			
			List<String> spectatingPlayers = this.spectators.get(s.toLowerCase());
			
			if (spectatingPlayers != null && spectatingPlayers.size() > 0) {
				
				for (String name : spectatingPlayers) {
					
					if (name.equalsIgnoreCase(pName)) {
						return s;
					}
					
				}
				
			}
					
		}
		
		// if no arena was found with the player spectating, return null
		return null;
		
	}
	
	/** Used to fetch the amount of spectators that are currently in an arena
	 * @param arenaName The arena in question
	 * @return Will return the amount of spectators in an arena, will return 0 if there are none
	 */
	public int getSpectatorAmount(String arenaName) {
		
		List<String> currentSpectators = this.spectators.get(arenaName.toLowerCase());
		
		if (currentSpectators == null || currentSpectators.size() < 1) {
			return 0;
		} else {
			return currentSpectators.size();
		}
		
	}
	
	/** Used to get the current spectators in an arena
	 * @param arenaName The arena to retrieve the spectators from
	 * @return Will return a string list of the spectators (will return null if there aren't any)
	 */
	public List<String> getSpectators(String arenaName) {
		
		return this.spectators.get(arenaName.toLowerCase());
		
	}
	
	/** Moves all spectators within an arena to lobby use the gamemodeSpectators to change them back
	 * @param arenaName The arena to move all the spectators in
	 */
	public void moveSpectatorsToLobby(String arenaName) {
		
		Location arenaLobby = this.getArenaLobby(arenaName);
		
		if (arenaLobby == null) {
			SurvivalGames.logger().logMessage("The players in arena '" + arenaName + "' could not be teleported to the lobby because the lobby location is null!", Condition.ERROR);
			return;
		}
		
		List<String> currentSpectators = this.getSpectators(arenaName);
		
		if (currentSpectators == null) return;
		
		for (String s : this.getSpectators(arenaName)) {
			
			Player inArena = Bukkit.getPlayer(s);
			
			if (s != null) {
				
				inArena.teleport(arenaLobby);
				
			}
			
		}
		
	}
	
	/** Used to set the gamemode of all spectators within an arena
	 * @param arenaName The arena in which to modify the spectators
	 * @param spectatorMode If set to true, all the spectators will be put into spectator mode, if false
	 * they will be put into survival
	 */
	public void gamemodeSpectators(String arenaName, boolean spectatorMode) {
		
		List<String> currentSpectators = this.getSpectators(arenaName);
		
		if (currentSpectators == null) return;
		
		for (String s : currentSpectators) {
			
			Player inArena = Bukkit.getPlayer(s);
			
			if (inArena != null) {
				
				if (spectatorMode) {
					inArena.setGameMode(GameMode.SPECTATOR);
				} else {
					inArena.setGameMode(GameMode.SURVIVAL);
				}
				
			}
			
		}
		
	}
	
	/** Used to remove all data on spectators in a particular arena (this method does not move them or de-gamemode them)
	 * @param arenaName The arena to clear out
	 */
	public void clearSpectators(String arenaName) {
		
		this.spectators.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to get a player's reputation by just their playername (requires uuidAPI to function correctly)
	 * @param playerName The string name of the player
	 * @return The specified player's reputation (if none stored, it will return the default reputation set in the config)
	 */
	public int getReputation(String playerName) {
		
		// TODO: add support for uuidAPI
		
		return Values.DEFAULT_REPUTATION;
		
	}
	
	/** Used to get the player's reputation by their UUID. It will automatically create a new entry if it doesn't exist
	 * @param uuid The player's UUID
	 * @return The specified player's reputation (if none stored, it will return the default reputation set in the config)
	 */
	public int getReputation(UUID uuid) {
		
		FileConfiguration config = SurvivalGames.getRepConfig().getCustomConfig();
		
		if (!config.isSet("reps." + uuid.toString())) {
			config.set("reps." + uuid.toString(), Values.DEFAULT_REPUTATION);
			SurvivalGames.getRepConfig().saveCustomConfig();
		}
		
		return config.getInt("reps." + uuid.toString(), Values.DEFAULT_REPUTATION);
		
	}
	
	/** Used to get a player's reputation by their player object
	 * @param player The player in question
	 * @return The specified player's reputation (if none stored, it will return the default reputation set in the config)
	 */
	public int getReputation(Player player) {
		
		FileConfiguration config = SurvivalGames.getRepConfig().getCustomConfig();
		
		if (!config.isSet("reps." + player.getUniqueId().toString())) {
			config.set("reps." + player.getUniqueId().toString(), Values.DEFAULT_REPUTATION);
			SurvivalGames.getRepConfig().saveCustomConfig();
		}
		
		return config.getInt("reps." + player.getUniqueId().toString(), Values.DEFAULT_REPUTATION);
		
	}
	
	/** Positively increments the specified player's reputation
	 * @param player The player who is getting the rep boost
	 */
	public void addReputation(Player player) {
		
		FileConfiguration config = SurvivalGames.getRepConfig().getCustomConfig();
		
		int playerRep = config.getInt("reps." + player.getUniqueId().toString(), Values.DEFAULT_REPUTATION);
		
		playerRep++;
		
		config.set("reps." + player.getUniqueId().toString(), playerRep);
		
		SurvivalGames.getRepConfig().saveCustomConfig();
		
	}
	
	/** Positively increments the specified player's reputation
	 * @param uuid The UUID of the player who is getting the rep boost
	 */
	public void addReputation(UUID uuid) {
		
		FileConfiguration config = SurvivalGames.getRepConfig().getCustomConfig();
		
		int playerRep = config.getInt("reps." + uuid.toString(), 0);
		
		playerRep++;
		
		config.set("reps." + uuid.toString(), playerRep);
		
		SurvivalGames.getRepConfig().saveCustomConfig();
		
	}
	
	/** Negatively increments the specified player's reputation (requires uuidAPI to function)
	 * @param playerName The string name of the player who is getting the rep decrease
	 */
	public void minusReputation(String playerName) {
		
		// TODO: add uuidAPI support
		
	}
	
	/** Negatively increments the specified player's reputation
	 * @param player The player who is getting the rep decrease
	 */
	public void minusReputation(Player player) {
		
		FileConfiguration config = SurvivalGames.getRepConfig().getCustomConfig();
		
		int playerRep = config.getInt("reps." + player.getUniqueId().toString(), 0);
		
		playerRep--;
		
		config.set("reps." + player.getUniqueId().toString(), playerRep);
		
		SurvivalGames.getRepConfig().saveCustomConfig();
		
	}
	
	/** Negatively increments the specified player's reputation
	 * @param uuid The UUID of the player who is getting the rep decrease
	 */
	public void minusReputation(UUID uuid) {
		
		FileConfiguration config = SurvivalGames.getRepConfig().getCustomConfig();
		
		int playerRep = config.getInt("reps." + uuid.toString(), 0);
		
		playerRep--;
		
		config.set("reps." + uuid.toString(), playerRep);
		
		SurvivalGames.getRepConfig().saveCustomConfig();
		
	}
	
	/** Positively increments the specified player's reputation (requires uuidAPI to function)
	 * @param playerName The string name of the player who is getting the rep boost
	 */
	public void addReputation(String playerName) {
		
		// TODO: add uuidAPI support
		
	}
	
	/** Used to set player's reputation
	 * @param player The player to set the rep for
	 * @param rep The new rep for the player
	 */
	public void setReputation(Player player, int rep) {
		
		FileConfiguration config = SurvivalGames.getRepConfig().getCustomConfig();
		
		config.set("reps." + player.getUniqueId().toString(), rep);
		
		SurvivalGames.getRepConfig().saveCustomConfig();
		
	}
	
	/** Used to set player's reputation
	 * @param uuid The UUID of the player to set the rep for
	 * @param rep The new rep for the player
	 */
	public void setReputation(UUID uuid, int rep) {
		
		FileConfiguration config = SurvivalGames.getRepConfig().getCustomConfig();
		
		config.set("reps." + uuid.toString(), rep);
		
		SurvivalGames.getRepConfig().saveCustomConfig();
		
	}
	
	/** Used to set player's reputation (requires uuidAPI to function)
	 * @param uuid The string name of the player to set the rep for
	 * @param rep The new rep for the player
	 */
	public void setReputation(String playerName, int rep) {
		
		// TODO: add uuidAPI support
		
	}
	
	/** Used to set the minimum reputation of an arena
	 * @param arenaName The name of the arena to set the minimum reputation for
	 * @param rep The new reputation
	 */
	public void setMinimumRep(String arenaName, int rep) {
		
		this.minimumReps.put(arenaName.toLowerCase(), rep);
		
	}
	
	/** Used to get an arena's minimum reputation (works even if minimum reputation is disabled!
	 * @param arenaName The name of the arena to fetch the reputation for
	 * @return Will return the minimum required reputation to join an arena! 
	 */
	public int getMinimumRep(String arenaName) {
		
		Integer minimumRep = this.minimumReps.get(arenaName.toLowerCase());
		
		if (minimumRep == null) {
			return 0;
		} else {
			return minimumRep;
		}
		
	}
	
	/** Used to set the center location of an arena
	 * @param arenaName The arena to set the center for
	 * @param formattedLocation The formatted center location. Format with {@link ArenaFile#formatString(World, double, double, double, float, float)}
	 */
	public void setCenterLocation(String arenaName, String formattedLocation) {
		
		if (formattedLocation == null || formattedLocation.split("//").length < 6) return;
		
		this.centerLocation.put(arenaName.toLowerCase(), formattedLocation);
		
	}
	
	/** Used to get the arena's center location
	 * @param arenaName The arena to get the center location for
	 * @return The de-formatted location of the arena's center. Will return null if is none stored
	 */
	public Location getCenterLocation(String arenaName) {
		
		String formattedCenterLocation = this.centerLocation.get(arenaName.toLowerCase());
		
		if (formattedCenterLocation == null) return null;
		
		return ArenaFile.deformatString(formattedCenterLocation);
		
	}

	/** Used to remove an arena's center location
	 * @param arenaName The name of the arena to remove the center location for
	 */
	public void removeCenterLocation(String arenaName) {
		
		this.centerLocation.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to determine a player's distance from the center of the arena they are currently in
	 * @param arenaName The arena to get the center for
	 * @param loc The location to check the distance of
	 * @return Will return the distance between two locations.. Will return 0 if awkward conditions are met ;)
	 */
	public double distanceFromCenter(String arenaName, Location loc) {
		
		if (!this.containsArena(arenaName)) return 0;
		
		Location center = this.getCenterLocation(arenaName);
		
		if (center.getWorld() != loc.getWorld()) return 0;

		return center.distance(loc);
		
	}
	
	/** Used to set an arena's default border radius
	 * @param arenaName The name of the arena to set the border radius for
	 * @param borderRadius The arena's border radius
	 */
	public void setDefaultBorderRadius(String arenaName, int borderRadius) {
		
		this.defaultBorderRadius.put(arenaName.toLowerCase(), borderRadius);
		
	}
	
	/** Used to get an arena's default border radius
	 * @param arenaName The name of the arena to fetch the radius for
	 * @return The arena's radius size. Will return 0 if none is defined
	 */
	public int getDefaultBorderRadius(String arenaName) {
		
		Integer radius = this.defaultBorderRadius.get(arenaName.toLowerCase());
		
		if (radius == null) {
			return 0;
		} else {
			return radius;
		}
		
	}
	
	/** Used to determine if the border radius for an arena has been set
	 * @param arenaName The arena to check
	 * @return Will return true if the border radius has been set
	 */
	public boolean isBorderRadiusSet(String arenaName) {
		
		return this.borderRadius.containsKey(arenaName.toLowerCase());
		
	}
	
	/** Used to set an arena's border radius
	 * @param arenaName The name of the arena to set the border radius for
	 * @param borderRadius The arena's border radius
	 */
	public void setBorderRadius(String arenaName, double borderRadius) {
		
		this.borderRadius.put(arenaName.toLowerCase(), borderRadius);
		
	}
	
	/** Used to get an arena's border radius
	 * @param arenaName The name of the arena to fetch the radius for
	 * @return The arena's radius size. Will return 0 if none is defined
	 */
	public double getBorderRadius(String arenaName) {
		
		Double radius = this.borderRadius.get(arenaName.toLowerCase());
		
		if (radius == null) {
			return 0;
		} else {
			return radius;
		}
		
	}
	
	/** Used in "cleaning up" arenas. Will remove the stored border radius. This is okay because the default border radius is still stored
	 * @param arenaName The arena to remove the border radius from
	 */
	public void removeBorderRadius(String arenaName) {
		
		this.borderRadius.remove(arenaName.toLowerCase());
		
	}
	
}
