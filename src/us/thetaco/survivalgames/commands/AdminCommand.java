package us.thetaco.survivalgames.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.file.ConfigHandler;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.Values;
import us.thetaco.survivalgames.utils.SimpleLogger.Condition;

/** A command for handling everything to do with arena administration
 * @author activates
 *
 */
public class AdminCommand implements CommandExecutor {

	private SurvivalGames plugin;
	private ConfigHandler config;
	public AdminCommand(SurvivalGames plugin, ConfigHandler config) {
		this.plugin = plugin;
		this.config = config;
	}
	
	private List<String> playersInArenaMode = new ArrayList<String>();
	
	// creating two hashmaps to store what player is editing what arena, and what spawnpoints are set for that specific arena
	// this map will hold the playername tied to the specified arena name.. if they are in arena creation mode, they should have a spot on here
	private Map<String, String> playersForArena = new HashMap<String, String>();
	// this will hold all of the spawnpoints for the specified arena by name
	private Map<String, List<String>> arenaSpawnPoints = new HashMap<String, List<String>>();
	// this will hold if a player is in the update mode or not
	private Map<String, Boolean> isUpdating = new HashMap<String, Boolean>();
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		// check if the sender is the console or not
		if (!(sender instanceof Player)) {
			SurvivalGames.logger().logMessage(Language.NO_CONSOLE_SUPPORT.toString(), Condition.NORMAL);
			return true;
		}
		
		Player player = (Player) sender;
		
		// checking to see if the player supplied any subcommands.. if they didn't, reprimand them for it
		if (args.length < 1) {
			this.printHelpMessage(player);
			return true;
		}
		
		if (args[0].equalsIgnoreCase("create")) {
			
			if (this.inArenaMode(player)) {
				player.sendMessage(Language.ALREADY_IN_CREATION_MODE.toString());
				return true;
			}
			
			if (!player.hasPermission("hungergames.commands.admin.create")) {
				player.sendMessage(Language.NO_PERMISSION.toString());
				return true;
			}
			
			// check if the player specified a name
			if (args.length < 2) {
				player.sendMessage(Language.NO_ARENA_SPECIFIED.toString());
				return true;
			}
			
			// checking if the string is alpha-numeric
			if (!StringUtils.isAlphanumeric(args[1])) {
				player.sendMessage(Language.ALPHA_NUMERIC_REQUIRED.toString());
				return true;
			}
			
			// checking if the arena already exists
			if (SurvivalGames.getArenaData().containsArena(args[1])) {
				player.sendMessage(Language.ARENA_ALREADY_EXISTS.toString());
				return true;
			}
			
			// add the player to the arenaMode list
			this.addPlayersToArenaMode(player);
			this.addPlayerInArenaCreation(player.getName().toLowerCase(), args[1]);
			this.setPlayerUpdating(player, false);
			// send them off to the subcommand class, so it can deal with messages
			new ArenaCreateSubCommand().setupToCreate(player);
			
			return true;
			
		} else if (args[0].equalsIgnoreCase("delete")) {
			
			if (this.inArenaMode(player)) {
				player.sendMessage(Language.ALREADY_IN_CREATION_MODE.toString());
				return true;
			}
			
			if (!player.hasPermission("hungergames.commands.admin.delete")) {
				player.sendMessage(Language.NO_PERMISSION.toString());
				return true;
			}
			
			// checking to see if the player supplied the arena name
			if (args.length < 2) {
				player.sendMessage(Language.NO_ARENA_SPECIFIED.toString());
				return true;
			}
			
			if (!StringUtils.isAlphanumeric(args[1])) {
				player.sendMessage(Language.ALPHA_NUMERIC_REQUIRED.toString());
				return true;
			}
			
			// sending the rest of the work to the subcommand class for deleting arenas
			new ArenaDeleteSubcommand().deleteArena(player, args[1], plugin);
			
			return true;
			
		} else if (args[0].equalsIgnoreCase("update")) {
			
			if (this.inArenaMode(player)) {
				player.sendMessage(Language.ALREADY_IN_CREATION_MODE.toString());
				return true;
			}
			
			if (!player.hasPermission("hungergames.commands.admin.update")) {
				player.sendMessage(Language.NO_PERMISSION.toString());
				return true;
			}
			
			// checking to see if the player supplied the arena name
			if (args.length < 2) {
				player.sendMessage(Language.NO_ARENA_SPECIFIED.toString());
				return true;
			}
			
			// checking if the string is alpha-numeric
			if (!StringUtils.isAlphanumeric(args[1])) {
				player.sendMessage(Language.ALPHA_NUMERIC_REQUIRED.toString());
				return true;
			}
			
			if (!SurvivalGames.getArenaData().containsArena(args[1])) {
				player.sendMessage(Language.ARENA_NOT_FOUND.toString());
				return true;
			}
			
			/*
			 * Since they are updating the arena.. we could have just them overwrite the old file. SO we can reuse the arenacreatesubcommand class
			 */
			
			// add the player to the arenaMode list
			this.addPlayersToArenaMode(player);
			this.addPlayerInArenaCreation(player.getName().toLowerCase(), args[1]);
			this.setPlayerUpdating(player, true);
			
			// Notify the player that they are now updating
			player.sendMessage(Language.IN_UPDATE_MODE.toString());
			
			return true;
			
		} else if (args[0].equalsIgnoreCase("reload")) {
			
			if (!player.hasPermission("hungergames.commands.admin.reload")) {
				player.sendMessage(Language.NO_PERMISSION.toString());
				return true;
			}
			
			player.sendMessage(Language.RELOADING_VALUES.toString());
			config.reloadValues();
			SurvivalGames.getRepConfig().reloadCustomConfig();
			
		} else if (args[0].equalsIgnoreCase("kick")) {
			
			// first we must check to make sure they have the required permissions
			if (!player.hasPermission("survivalgames.commands.admin.kick")) {
				player.sendMessage(Language.NO_PERMISSION.toString());
				return true;
			}
			
			// now to check if they have supplied a player
			if (args.length < 2) {
				player.sendMessage(Language.NO_PLAYER_SPECIFIED.toString());
				return true;
			}
			
			Player target = Bukkit.getPlayer(args[1]);
			
			// check if the player they specified is online
			if (target == null) {
				player.sendMessage(Language.PLAYER_NOT_FOUND.toString(null, null, null, args[1]));
				return true;
			}
			
			// check to see if the player is in any arena or lobby
			String arenaName = SurvivalGames.getLobbyHandler().getPlayerArenaName(target);
			
			if (arenaName != null) {
				
				// This will run if the player is in a lobby
				
				// we now know that the player is in a lobby and the name of the arena they are in, so we can just remove them from the lobby!
				SurvivalGames.getLobbyHandler().removePlayerFromLobby(arenaName, target);
				
				// message the other players that the specified player has been kicked from the game!
				SurvivalGames.getLobbyHandler().messagePlayersInLobby(arenaName, Language.KICK_BROADCAST_MESSAGE.toString(null, target, null, null));
				
				// move the player back to the lobby spawn even though they might still be in the lobby spawn
				SurvivalGames.getArenaData().movePlayerToLobby(arenaName, target);
				
				// notify the player that they have been kicked
				target.sendMessage(Language.KICK_MESSAGE.toString(arenaName, null, null, null));
				
				// message the sender that the player has been removed sucessfully
				player.sendMessage(Language.KICK_SUCCESS.toString(arenaName, target, null, null));
				
				return true;
			}
			
			// check if player is in arena to kick
			
			arenaName = SurvivalGames.getArenaData().playerInArena(target, false);
			
			if (arenaName != null) {
				
				// this will run if the player is in an arena game
				
				// start by removing the player from the game
				SurvivalGames.getArenaData().removePlayerFromArena(arenaName, target);
				
				// strike lightning where the target is standing for cool effects B)
				if (Values.LIGHTNING_ENABLED) {
					
					target.getLocation().getWorld().strikeLightningEffect(target.getLocation());

				}
				
				// teleport them to the arena lobby
				SurvivalGames.getArenaData().movePlayerToLobby(arenaName, target);
				
				// restore their inventory
				SurvivalGames.getArenaData().restoreInventory(target);
				
				// broadcast the kick to the game!
				SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.KICK_BROADCAST_MESSAGE.toString(null, target, null, null), false);
				
				// tell the target that they have been kicked
				target.sendMessage(Language.KICK_MESSAGE.toString(arenaName, null, null, null));
				
				// remove the player's scoreboard
				SurvivalGames.getArenaHandler().removeScoreboard(target);
				
				// check if the arena still has enough players in it to continue
				int alivePlayerCount = SurvivalGames.getArenaData().getAmountOfPlayerInArena(arenaName, false);
				
				if (alivePlayerCount < 2) {
					
					// This will run if there is only one player left (or there was only one player in the arena and they were kicked)
						
					// get last player
					Set<String> playersInArena = SurvivalGames.getArenaData().getPlayersInArena(arenaName, false);
					String nameOfWinner = "nobody";
					
					if (playersInArena != null && !playersInArena.isEmpty()) {
						nameOfWinner = playersInArena.iterator().next();
					}
					
					// Since there is now only 1 player left... announce the winning of the game
					SurvivalGames.getArenaData().messagePlayersInArena(arenaName, Language.GAME_WON_KICK.toString(arenaName, null, null, nameOfWinner), true);
					
					// clear the arena
					SurvivalGames.getArenaHandler().closeGame(arenaName);
						
				}
				
				return true;
				
			}
			
			player.sendMessage(Language.NOTHING_TO_KICK_FROM.toString(null, player, null, null));
			
		} else {
			
			// An unknown subcommand was given, so just print out the list
			this.printHelpMessage(player);
			return true;
		}
			
		return true;
		
	}

	/** Will add the specified player to an "arena" mode. When in this arena mode, they will be unable to chat
	 * @param player The player to add to arena mode
	 */
	public void addPlayersToArenaMode(Player player) {
		
		playersInArenaMode.add(player.getName().toLowerCase());
		
	}
	
	/** Used for setting if a player is in the update mode or not
	 * @param player The player to set updating
	 * @param isUpdating Set to true if the player needs to be put in update mode
	 */
	public void setPlayerUpdating(Player player, Boolean isUpdating) {
		
		this.isUpdating.put(player.getName().toLowerCase(), isUpdating);
		
	}
	
	/** Used to check and see if the certain player is in the update or not
	 * @param player The player in question
	 * @return Will return true if the player is in updating mode
	 */
	public boolean isUpdating(Player player) {
		
		Boolean isPlayerUpdating = this.isUpdating.get(player.getName().toLowerCase());
		
		if (isPlayerUpdating == null || !isPlayerUpdating) {
			return false;
		} else {
			return true;
		}
		
	}
	
	
	/** Used to remove a player updating (for cleanup purposes)
	 * @param player The player to remove from the update process
	 */
	public void removePlayerUpdating(Player player) {
		
		this.isUpdating.remove(player.getName().toLowerCase());
		
	}
	
	/** Removes the specified player from the arena mode
	 * @param player The player to remove from arena mode
	 */
	public void removePlayersInArenaMode(Player player) {
		
		playersInArenaMode.remove(player.getName().toLowerCase());
		
	}
	
	/** Prints a help message to the specified
	 * @param player The player that needs the help message
	 */
	private void printHelpMessage(Player player) {
		
		// compile a list of permissions that the player is able to use:
		List<String> availableCommands = new ArrayList<String>();
		
		if (player.hasPermission("hungergames.commands.admin.create")) {
			availableCommands.add(ChatColor.GOLD + "/hgadmin " + ChatColor.RED + "create <name> " + ChatColor.GRAY + "-" + ChatColor.GOLD + " begins the creation process of an arena");
		}

		if (player.hasPermission("hungergames.commands.admin.delete")) {
			availableCommands.add(ChatColor.GOLD + "/hgadmin " + ChatColor.RED + "delete <name> " + ChatColor.GRAY + "-" + ChatColor.GOLD + " deletes the specified arena");
		}
		
		if (player.hasPermission("hungergames.commands.admin.update")) {
			availableCommands.add(ChatColor.GOLD + "/hgadmin " + ChatColor.RED + "update <name> " + ChatColor.GRAY + "-" + ChatColor.GOLD + " allows you to update the specified arena");
		}
		
		if (!player.hasPermission("hungergames.commands.admin.reload")) {
			availableCommands.add(ChatColor.GOLD + "/hgadmin " + ChatColor.RED + "reload <name> " + ChatColor.GRAY + "-" + ChatColor.GOLD + " updates the values from the config file");
		}

		if (!player.hasPermission("hungergames.commands.admin.kick")) {
			availableCommands.add(ChatColor.GOLD + "/hgadmin " + ChatColor.RED + "kick <name> " + ChatColor.GRAY + "-" + ChatColor.GOLD + " kicks the specified player from their current game");
		}
		
		player.sendMessage(ChatColor.RED + "=============(Survival-Games)=============");

		if (availableCommands.size() < 1) {
			player.sendMessage(ChatColor.RED + "You have no available commands");
		} else {
			
			for (String s : availableCommands) {
				player.sendMessage(s);
			}
			
		}
		
	}
	
	/** A method for determining if the specified player is in arena mode
	 * @param player The player in question
	 * @return This method will return true if the player is in arena-mode
	 */
	public boolean inArenaMode(Player player) {
		
		if (playersInArenaMode.contains(player.getName().toLowerCase())) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/** Used for setting the player and the arena they are creating
	 * @param playerName The player who is creating the arena
	 * @param arenaName The name of the arena that the specified player is creating
	 */
	public void addPlayerInArenaCreation(String playerName, String arenaName) {
		playersForArena.put(playerName.toLowerCase(), arenaName);
	}
	
	/** Will return the name of the arena that the specified player is creating.. if any
	 * @param playerName The name of the player that is creating the arena
	 * @return The name of the arena that the player is creating (if any)
	 */
	public String getArenaForPlayerInCreation(String playerName) {
		return playersForArena.get(playerName.toLowerCase());
	}
	
	/** Used for adding another spawnpoint to the arena before committing it to the file
	 * @param arenaName The name of the arena to update the info for
	 * @param spawnpointInfo The compiled spawn-point info
	 */
	public void addToSpawnPoints(String arenaName, String spawnpointInfo) {
		
		if (arenaSpawnPoints.get(arenaName) == null) {
			
			List<String> toAdd = new ArrayList<String>();
			toAdd.add(spawnpointInfo);
			
			arenaSpawnPoints.put(arenaName, toAdd);
			
		} else {
			
			List<String> oldInfo = arenaSpawnPoints.get(arenaName);
			
			oldInfo.add(spawnpointInfo);
			
			arenaSpawnPoints.put(arenaName, oldInfo);
			
		}
		
	}
	
	/** A "getter" method for the spawnpoint info
	 * @param arenaName The arena in question
	 * @return The entire list of all the spawnpoints for the arena
	 */
	public List<String> getSpawnPointInfo(String arenaName) {
		
		return arenaSpawnPoints.get(arenaName);
		
	}

	/** A sort of clean-up method. This should be ran if the player disconnects or stops
	 * the arena creation process in any other way
	 * @param playerName The player to clean-up after
	 */
	public void removePlayerAndArena(String playerName) {
		
		String arenaName = this.playersForArena.get(playerName.toLowerCase());
		
		if (arenaName != null) {
			
			this.arenaSpawnPoints.remove(arenaName);
			
		}
		
		this.playersForArena.remove(playerName);
		
	}
	
}
