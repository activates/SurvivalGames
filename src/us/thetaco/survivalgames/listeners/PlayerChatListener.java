package us.thetaco.survivalgames.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.commands.AdminCommand;
import us.thetaco.survivalgames.file.ArenaFile;
import us.thetaco.survivalgames.utils.Language;

/** The class that will be used for handling chat during arena creation.
 * We brought over the arenaCommand class because we need to use the getters
 * within that class
 * 
 * @author activates
 *
 */
public class PlayerChatListener implements Listener {

	private Map<String, Integer> mpCounts = new HashMap<String, Integer>();
	private Map<String, String> lobbyLocations = new HashMap<String, String>();
	private Map<String, Integer> lobbyTime = new HashMap<String, Integer>();
	private Map<String, Integer> arenaTime = new HashMap<String, Integer>();
	private Map<String, Integer> requiredRep = new HashMap<String, Integer>(); 
	private List<String> isLoaded = new ArrayList<String>();
	private Map<String, String> centerLocations = new HashMap<String, String>();
	private Map<String, Integer> arenaRadius = new HashMap<String, Integer>();
	
	private AdminCommand adminCommand;
	private ArenaFile aFile;
	public PlayerChatListener(AdminCommand adminCommand, ArenaFile aFile) {
		this.adminCommand = adminCommand;
		this.aFile = aFile;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		
		Player player = e.getPlayer();
		
		// stopping the method in its tracks if the player isn't in arena mode
		if (!adminCommand.inArenaMode(player)) return;
			
		// preventing any chat messages from being sent
		e.setCancelled(true);
			
		// loading everything if the player is in the update mode
		if (this.adminCommand.isUpdating(player) && !this.isPlayerLoaded(player)) {
			
			// this will run if the player is in update mode, but their information their selected arena hasn't been uploaded this
			// creation process, yet. We'll do that here
			
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			this.setMPCount(arenaName, SurvivalGames.getArenaData().getMinimumPayerCount(arenaName));
			this.setLobbyLocation(arenaName, SurvivalGames.getArenaData().getArenaLobby(arenaName));
			this.setLobbyTime(arenaName, SurvivalGames.getArenaData().getLobbyTime(arenaName));
			this.setArenaTime(arenaName, SurvivalGames.getArenaData().getArenaTime(arenaName));
			this.setCenterLocation(arenaName, SurvivalGames.getArenaData().getCenterLocation(arenaName));
			this.setArenaRadius(arenaName, SurvivalGames.getArenaData().getDefaultBorderRadius(arenaName));
			this.setMinimumRep(arenaName, SurvivalGames.getArenaData().getMinimumRep(arenaName));
			
			// Adding the spawn points
			for (String s : SurvivalGames.getArenaData().getSpawnPoints(arenaName)) {
				adminCommand.addToSpawnPoints(arenaName, s);
			}
			
			// at this point everything should be loaded, so add that the player is loaded
			this.addLoadedPlayer(player);
			
		}
		
		String[] args = e.getMessage().split(" ");
		
		if (args[0].equalsIgnoreCase("exit")) {
			// if the player chooses to exit, we'll stop the event here
			adminCommand.removePlayersInArenaMode(player);
			
			player.sendMessage(Language.CREATION_MODE_DISABLED.toString());
			
			// cleaning up after the player
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			this.removeMPCount(arenaName);
			this.removeLobbyLocation(arenaName);
			this.removeLobbyTime(arenaName);
			this.removeArenaTime(arenaName);
			this.removeCenterLocation(arenaName);
			this.removeMinimumRep(arenaName);
			adminCommand.removePlayerAndArena(player.getName());
			
			this.removedLoadedPlayer(player);
			this.adminCommand.removePlayerUpdating(player);
			
			return;
		} else if (args[0].equalsIgnoreCase("setspawn")) {
			
			// this will run if the player just types setspawn and nothing else
			
			Location loc = player.getLocation();
			
			String formattedString = ArenaFile.formatString(loc.getWorld() ,loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			adminCommand.addToSpawnPoints(arenaName, formattedString);
			Integer spawnPointNumber = adminCommand.getSpawnPointInfo(arenaName).size();
			
			player.sendMessage(Language.SPAWN_SET_NUMBER.toString(null, null, spawnPointNumber, null));
			
			return;
			
		} else if (args[0].equalsIgnoreCase("finish")) {
			
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			if (!this.isMPCountSet(arenaName)) {
				player.sendMessage(Language.MP_COUNT_NOT_SET.toString());
				return;
			}
			
			// checking if the mp count is too low...
			if (!this.isMPCountValid(arenaName)) {
				player.sendMessage(Language.MP_COUNT_INVALID.toString());
				return;
			}
			
			// this will run if the player types finish. When they type finish, it will store the arena in a file
			player.sendMessage(Language.ARENA_CREATION_FINISHED.toString());
			
			List<String> spawns = adminCommand.getSpawnPointInfo(arenaName);
			
			if (spawns == null || spawns.size() < 1) {
				player.sendMessage(Language.NO_SPAWNS_DEFINED.toString());
				adminCommand.removePlayerAndArena(player.getName());
				adminCommand.removePlayersInArenaMode(player);
				return;
			}
			
			String formattedLobbyLocation = this.getLobbyLocation(arenaName);
			
			if (formattedLobbyLocation == null) {
				player.sendMessage(Language.NO_LOBBY_DEFINED.toString());
				return;
			}
			
			Integer lobbyTime = this.getLobbyTime(arenaName);
			
			if (!this.isLobbyTimeSet(arenaName)) {
				player.sendMessage(Language.NO_LOBBY_TIME.toString());
				return;
			}
			
			Integer arenaTime = this.getArenaTime(arenaName);
			
			if (!this.isArenaTimeSet(arenaName)) {
				player.sendMessage(Language.NO_ARENA_TIME.toString());
				return;
			}
			
			Integer minimumRep = this.getMinimumRep(arenaName);
			
			if (!this.isMinimumRepSet(arenaName)) {
				player.sendMessage(Language.MINIMUM_REP_NOT_SET.toString());
				return;
			}
			
			String formattedCenterLocation = this.getCenterLocation(arenaName);
			
			if (!this.isCenterLocationSet(arenaName)) {
				player.sendMessage(Language.CENTER_LOCATION_NOT_SET.toString());
				return;
			}
			
			int radiusSize = this.getArenaRadius(arenaName);
			
			if (!this.isArenaRadiusSet(arenaName)) {
				player.sendMessage(Language.ARENA_RADIUS_NOT_SET.toString());
				return;
			}
			
			aFile.createArena(arenaName, spawns, this.getMPCount(arenaName), ArenaFile.deformatString(formattedLobbyLocation), lobbyTime, arenaTime, minimumRep, formattedCenterLocation, radiusSize);			
			
			SurvivalGames.getArenaData().setArenaLobby(arenaName, this.getLobbyLocation(arenaName));
			
			player.sendMessage(Language.ARENA_CREATION_FINISHED.toString());
			
			adminCommand.removePlayerAndArena(player.getName().toLowerCase());
			adminCommand.removePlayersInArenaMode(player);
			adminCommand.removePlayerUpdating(player);
			
			this.removeMPCount(arenaName);
			this.removeLobbyLocation(arenaName);
			
			// checking to make sure the arena doesn't already exist (because of the update command)
			if (!SurvivalGames.getArenaData().containsArena(arenaName)) {
				// adding the arena to memory
				SurvivalGames.getArenaData().addArena(arenaName, spawns);
			} else {
				
				// just making sure the old arena would get overwritten.. because if we didn't do this
				// it wouldn't actively update the arena spawn-points until the server was restarted
				SurvivalGames.getArenaData().removeArena(arenaName);
				SurvivalGames.getArenaData().addArena(arenaName, spawns);
				
			}
			
			for (String s : spawns) {
				
				player.sendMessage(s);
				
			}
			
			// setting the game times
			SurvivalGames.getArenaData().setLobbyTime(arenaName, lobbyTime);
			SurvivalGames.getArenaData().setArenaTime(arenaName, arenaTime);
			
			// setting the center location
			SurvivalGames.getArenaData().setCenterLocation(arenaName, formattedCenterLocation);
			
			// setting the radius
			SurvivalGames.getArenaData().setDefaultBorderRadius(arenaName, radiusSize);
			
			this.removeLobbyTime(arenaName);
			this.removeArenaTime(arenaName);
			this.removedLoadedPlayer(player);
			this.removeCenterLocation(arenaName);
			this.removeArenaRadius(arenaName);			
			
			this.removedLoadedPlayer(player);
			this.adminCommand.removePlayerUpdating(player);
			
		} else if (args[0].equalsIgnoreCase("setmpcount")) {
		
			if (args.length < 2) {
				player.sendMessage(Language.MP_COUNT_NOT_SET.toString());
				return;
			}
			
			int mpCount = 0;
			
			try {
				
				mpCount = Integer.parseInt(args[1]);
				
				if (mpCount < 1) {
					player.sendMessage(Language.MP_COUNT_ERROR.toString());
					return;
				}
				
			} catch (Exception ex) {
				player.sendMessage(Language.MP_COUNT_WRONG_ARGS.toString(null, null, null, args[1]));
				return;
			}
			
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			this.setMPCount(arenaName, mpCount);
			
			player.sendMessage(Language.MP_COUNT_SET.toString(null, null, mpCount, null));
			return;
			
		} else if (args[0].equalsIgnoreCase("setlobby")) {
		
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			Location loc = player.getLocation();
			
			this.setLobbyLocation(arenaName, loc);
			
			player.sendMessage(Language.LOBBY_SET.toString());
			
		} else if (args[0].equalsIgnoreCase("setlobbytime")) {
			
			if (args.length < 2) {
				player.sendMessage(Language.LOBBY_TIME_SET_ERROR.toString());
				return;
			}
			
			List<String> toPass = new ArrayList<String>();
			
			int i = 0;
			for (String s : args) {
				if (i > 0) {
					toPass.add(s);
				}
				i++;
			}
			
			Integer parsedTime = this.parstTime(toPass);
			
			if (parsedTime == null) {
				
				player.sendMessage(Language.TIME_UNPARSEABLE.toString());
				return;
				
			}
			
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			this.setLobbyTime(arenaName, parsedTime);
			
			player.sendMessage(Language.LOBBY_TIME_SET.toString());
			
		} else if (args[0].equalsIgnoreCase("setarenatime")) {
			
			if (args.length < 2) {
				player.sendMessage(Language.ARENA_TIME_SET_ERROR.toString());
				return;
			}
			
			List<String> toPass = new ArrayList<String>();
			
			int i = 0;
			for (String s : args) {
				if (i > 0) {
					toPass.add(s);
				}
				i++;
			}
			
			Integer parsedTime = this.parstTime(toPass);
			
			if (parsedTime == null) {
				
				player.sendMessage(Language.TIME_UNPARSEABLE.toString());
				return;
				
			}
			
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			this.setArenaTime(arenaName, parsedTime);
			
			player.sendMessage(Language.ARENA_TIME_SET.toString());
			
		} else if (args[0].equalsIgnoreCase("setrequiredrep")) {
				
			if (args.length < 2) {
				player.sendMessage(Language.REP_SET_WRONG_ARGS.toString());
				return;
			}
			
			int i = 0;
			
			try {
				
				i = Integer.parseInt(args[1]);
				
			} catch (Exception ex) {
				player.sendMessage(Language.REP_INCORRECT_ARGS.toString(null, null, null, args[1]));
				return;
			}
			
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			this.setMinimumRep(arenaName, i);
			
			player.sendMessage(Language.REP_SET_SUCESSFULL.toString());
			
		} else if (args[0].equalsIgnoreCase("setcenter")) {
			
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			this.setCenterLocation(arenaName, player.getLocation());
			
			player.sendMessage(Language.CENTER_LOCATION_SET.toString());
			
		} else if (args[0].equalsIgnoreCase("setradius")) {
			
			String arenaName = adminCommand.getArenaForPlayerInCreation(player.getName().toLowerCase());
			
			if (!this.isCenterLocationSet(arenaName) && args.length < 2) {
				player.sendMessage(Language.CENTER_LOCATION_MUST_BE_SET_RADIUS.toString());
				return;
			} else if (args.length >= 2) {
				
				int pDefinedRadius = 0;
				
				try {
					
					pDefinedRadius = Integer.parseInt(args[1]);
					
				} catch (Exception ex) {
					Language.NON_NUMBER_DEFINED.toString(null, null, null, args[1]);
					return;
				}
				
				this.setArenaRadius(arenaName, pDefinedRadius);
				player.sendMessage(Language.ARENA_RADIUS_SET.toString(null, null, pDefinedRadius, null));
				
				return;
			}
			
			// this will run if we are to determine the player's distance from the center, then set that as the radius
			
			Location centerLocation = ArenaFile.deformatString(this.getCenterLocation(arenaName));
			Location playerLocation = player.getLocation();
			
			// make sure they are in the same world!
			if (!centerLocation.getWorld().getName().equalsIgnoreCase(playerLocation.getWorld().getName())) {
				Language.WORLDS_DONT_MATCH.toString();
				return;
			}
			
			int aDefinedRadius = (int) Math.ceil(centerLocation.distance(playerLocation));
			
			this.setArenaRadius(arenaName, aDefinedRadius);
			player.sendMessage(Language.ARENA_RADIUS_SET.toString(null, null, aDefinedRadius, null));
			
		} else {
			
			player.sendMessage(Language.UNKNOWN_COMMAND.toString());
			return;
		}
			
		
		
	}
		
	/** Used to parse time codes from a command
	 * @param input A string of the parsed time (such as: "2m 60s")
	 * @return Will return the total seconds from the amount of parsed time. Will return null if the input was invalid
	 */
	private Integer parstTime(List<String> input) {
		
		if (input.size() < 1) return null;
		
		int totalSeconds = 0;
		
		for (String s : input) {
			
			String[] split = s.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
			
			if (split.length < 2) return null;
			
			int time = 0;
			String type = new String();
			
			try {
				
				time = Math.abs(Integer.parseInt(split[0]));
				type = split[1];
				
			} catch (Exception e) {
				return null;
			}
			
			int newCalculation = 0;
			
			if (type.equalsIgnoreCase("h")) {
				
				newCalculation = time * 3600;
				
			} else if (type.contentEquals("m")) {
				
				newCalculation = time * 60;
				
			} else if (type.contentEquals("s")) {
				
				newCalculation = time;
				
			} else {
				
				return null;
				
			}
			
			totalSeconds = totalSeconds + newCalculation;
			
		}
		
		return totalSeconds;
		
	}
	
	/** Used to set the specified arena's lobby location, but only for the arena creation process. Use {@link SurvivalGames#getArenaData()#setLobbyLocation(String, Location)}
	 * to set the in-memory lobby location
	 * @param arenaName The name of the arena
	 * @param loc The location that the lobby will be set at (In Bukkit's format)
	 */
	private void setLobbyLocation(String arenaName, Location loc) {
		
		this.lobbyLocations.put(arenaName.toLowerCase(), ArenaFile.formatString(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
		
	}
	
	/** Used to get the stored lobby location
	 * @param arenaName The name of the arena
	 * @return The location of the specified arena's lobby. Will return null if it hasn't been stored, yet.
	 */
	private String getLobbyLocation(String arenaName) {
		
		return this.lobbyLocations.get(arenaName.toLowerCase());
		
	}
	
	/** Used to remove the lobby stored in this class
	 * @param arenaName The arena name to clean up.
	 */
	public void removeLobbyLocation(String arenaName) {
		
		this.lobbyLocations.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to set the minimum player count within this class. Only for arena creation purposes, if you need to set the minimum player class, do it with the
	 * ArenaData class
	 * @param arenaName The name of the arena to set the minimum player count
	 * @param mpCount The new minimum player count
	 */
	private void setMPCount(String arenaName, int mpCount) {
		
		this.mpCounts.put(arenaName.toLowerCase(), mpCount);
		
	}
	
	/** A method for "clean-up." Will remove the MP count once the arena is created
	 * @param arenaName The name of the arena to remove the MP count
	 */
	public void removeMPCount(String arenaName) {
		
		this.mpCounts.remove(arenaName.toLowerCase());
		
	}
	
	/** Checks if the mp count is set. Mainly used for the finishing of the arena creation process
	 * @param arenaName The name of the arena in question
	 * @return Will return true if the MP (minimum player) count has been set
	 */
	private boolean isMPCountSet(String arenaName) {
		
		return this.mpCounts.containsKey(arenaName.toLowerCase());
		
	}
	
	/** Simply used to retrieve the set MP count
	 * @param arenaName The name of the arena in question
	 * @return Will return if the MP count if it has been stored. Will return 1 if it hasn't been set (a type of fail-safe).
	 */
	private int getMPCount(String arenaName) {
		
		Integer mpCount = this.mpCounts.get(arenaName.toLowerCase());
		
		if (mpCount == null) {
			return 1;
		}
		
		return mpCount;
		
	}
	
	/** Used to determine if the Minimum player count is valid (it will be invalid if the mp count is higher than the amount of spawn points)
	 * @param arenaName The arena name to check
	 * @return Will return true if the MP count is invalid
	 */
	private boolean isMPCountValid(String arenaName) {
		
		if (!this.isMPCountSet(arenaName)) return false;
		
		int mpCount = this.getMPCount(arenaName);
		List<String> spawnPointCount = adminCommand.getSpawnPointInfo(arenaName);
		
		if (spawnPointCount == null) return false;
		
		if (mpCount > spawnPointCount.size()) {
			return false;
		} else {
			return true;
		}
		
		
	}
	
	/** Used to set the amount of lobby time in seconds
	 * @param arenaName The name of the arena
	 * @param lobbyTime The time for the lobby to last in seconds
	 */
	private void setLobbyTime(String arenaName, int lobbyTime) {
		
		this.lobbyTime.put(arenaName.toLowerCase(), lobbyTime);
		
	}
	
	/** Used to get the stored lobby time in seconds
	 * @param arenaName The name of the arena
	 * @return The time for a lobby to last in seconds
	 */
	private Integer getLobbyTime(String arenaName) {
		
		Integer toReturn = this.lobbyTime.get(arenaName.toLowerCase());
		
		if (toReturn == null || toReturn < 1) {
			
			return null;
			
		} else {
			
			return toReturn;
			
		}
		
	}
	
	/** Used to set the arena time in seconds
	 * @param arenaName The name of the arena
	 * @param arenaTime The amount of time an arena game will last in seconds
	 */
	private void setArenaTime(String arenaName, int arenaTime) {
		
		this.arenaTime.put(arenaName.toLowerCase(), arenaTime);
		
	}
	
	/** Used to get the arena time in seconds
	 * @param arenaName The name of the arena
	 * @return The amount of time an arena game will last in seconds
	 */
	private Integer getArenaTime(String arenaName) {
		
		Integer toReturn = this.arenaTime.get(arenaName.toLowerCase());
		
		if (toReturn == null || toReturn < 1) {
			
			return null;
			
		} else {
			
			return toReturn;
			
		}
		
	}
	
	/** Used to check if a lobby time has been set for this instance
	 * @param arenaName The name of the arena
	 * @return Will return true if the name has been set in the map
	 */
	private boolean isLobbyTimeSet(String arenaName) {
		
		if (this.lobbyTime.get(arenaName.toLowerCase()) == null) {
			return false;
		} else {
			return true;
		}
		
	}
	
	/** Used to check if an arena time has been set for this instance
	 * @param arenaName The name of the arena
	 * @return Will return true if the name has been set in the map
	 */
	private boolean isArenaTimeSet(String arenaName) {
		
		if (this.arenaTime.get(arenaName.toLowerCase()) == null) {
			return false;
		} else {
			return true;
		}
		
	}
	
	/** Used to remove a lobby time stored in the map
	 * @param arenaName The name of the arena
	 */
	public void removeLobbyTime(String arenaName) {
		
		this.lobbyTime.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to remove an arena time stored in the map
	 * @param arenaName The name of the arena
	 */
	public void removeArenaTime(String arenaName) {
		
		this.arenaTime.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to add a player who has sucessfully loaded the arena's information (used for updating arenas)
	 * @param p The player to add
	 */
	private void addLoadedPlayer(Player p) {
		
		this.isLoaded.add(p.getName().toLowerCase());
		
	}
	
	/** Used to determine if a player has loaded the arena's information (used for updating arenas)
	 * @param p The player in question
	 * @return Will return true if the player's information has been loaded
	 */
	private boolean isPlayerLoaded(Player p) {
		
		boolean doesContain = false;
		
		String playerName = p.getName();
		
		for (String s : this.isLoaded) {
			
			if (s.equalsIgnoreCase(playerName)) {
				doesContain = true;
				break;
			}
			
		}
		
		return doesContain;
		
	}
	
	/** Add a player who has had the arena's information loaded into their creation process
	 * @param p The player to add
	 */
	public void removedLoadedPlayer(Player p) {
		
		this.isLoaded.remove(p.getName().toLowerCase());
		
	}
	
	/** Used to set the minimum reputation in the creation process
	 * @param arenaName The arena to set the rep for
	 * @param rep The new minium rep
	 */
	private void setMinimumRep(String arenaName, int rep) {
		
		this.requiredRep.put(arenaName.toLowerCase(), rep);
		
	}
	
	/** Used to fetch the minimum reputation for the arena creation process
	 * @param arenaName The arena to fetch the minimum rep for
	 * @return Will return the minimum reputation of the specified arena if it set, if not it will return 0
	 */
	private int getMinimumRep(String arenaName) {
		
		Integer toReturn = this.requiredRep.get(arenaName.toLowerCase());
		
		if (toReturn == null) {
			return 0;
		} else {
		
			return this.requiredRep.get(arenaName.toLowerCase());
		
		}
		
	}
	
	/** Used to check if the minimum reputation is set for an arena
	 * @param arenaName The arena to check for
	 * @return If the minimum reputation has been set, it will return true.. If not it will return false
	 */
	private boolean isMinimumRepSet(String arenaName) {
		
		if (this.requiredRep.get(arenaName.toLowerCase()) == null) {
			return false;
		} else {
			return true;
		}
		
	}
	
	/** Used to remove the minimum rep at the end of the arena creation process
	 * @param arenaName The arena to remove the reputation for
	 */
	public void removeMinimumRep(String arenaName) {
		
		this.requiredRep.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to get the center location for the arena (in arena creation
	 * @param arenaName The name of the arena being created
	 * @param centerLoc The current loction of the player that will be used to get the arena's center point
	 */
	private void setCenterLocation(String arenaName, Location centerLoc) {
		
		this.centerLocations.put(arenaName.toLowerCase(), ArenaFile.formatString(centerLoc.getWorld(), centerLoc.getX(), centerLoc.getY(), centerLoc.getZ(), centerLoc.getYaw(), centerLoc.getPitch()));
		
	}
	
	/** Used to clear the arena's stored center location in the arena creation process
	 * @param arenaName The arena's name
	 */
	public void removeCenterLocation(String arenaName) {
		
		this.centerLocations.remove(arenaName.toLowerCase());
		
	}
	
	/** Used to get the center location is the arena creation process
	 * @param arenaName The arena's name
	 * @return The formatted version of the stored center location
	 */
	private String getCenterLocation(String arenaName) {
		
		return this.centerLocations.get(arenaName.toLowerCase());
		
	}
	
	/** Used to determine if the center location has been set, yet.
	 * @param arenaName The arena's name
	 * @return Will return true if the arena's location is set
	 */
	private boolean isCenterLocationSet(String arenaName) {
		
		if (this.centerLocations.get(arenaName.toLowerCase()) == null) {
			return false;
		} else {
			return true;
		}
		
	}
	
	/** Used to set the arena's radius for the arena creation process
	 * @param arenaName The name of the arena to temporarily set the radius for
	 * @param arenaRadius The desired radius
	 */
	private void setArenaRadius(String arenaName, Integer arenaRadius) {
		
		this.arenaRadius.put(arenaName.toLowerCase(), arenaRadius);
		
	}
	
	/** Used to fetch the arena's radius for the arena creation process
	 * @param arenaName The name of the arena
	 * @return An int representing the arena's set radius. Will return 0 if none was defined
	 */
	private int getArenaRadius(String arenaName) {
		
		Integer radius = this.arenaRadius.get(arenaName.toLowerCase());
		
		if (radius == null) {
			return 0;
		} else {
			return radius;
		}
		
	}
	
	/** Used to determine if the arena's radius is set for the arena creation process
	 * @param arenaName The arena in question
	 * @return Will return true if the arena's radius has been set in the arena creation process
	 */
	private boolean isArenaRadiusSet(String arenaName) {
		
		return this.arenaRadius.containsKey(arenaName.toLowerCase());
		
	}
	
	/** Used to remove an arena's radius (used for cleanup after the arena creation process comes to an end)
	 * @param arenaName The name of the arena to "clean up" after
	 */
	public void removeArenaRadius(String arenaName) {
		
		this.arenaRadius.remove(arenaName.toLowerCase());
		
	}
	
}
