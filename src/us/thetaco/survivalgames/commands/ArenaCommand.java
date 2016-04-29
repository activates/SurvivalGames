package us.thetaco.survivalgames.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.SimpleLogger.Condition;
import us.thetaco.survivalgames.utils.Values;

/** A class for handling the arena command.. This class will hand out some of its processes
 * to sub-command classes if needed
 * @author activates
 *
 */
public class ArenaCommand implements CommandExecutor {	
	
	private AdminCommand adminCmd;
	public ArenaCommand(AdminCommand adminCmd) {
		this.adminCmd = adminCmd;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		// check if the sender is the console or not
		if (!(sender instanceof Player)) {
			SurvivalGames.logger().logMessage(Language.NO_CONSOLE_SUPPORT.toString(), Condition.WARNING);
			return true;
		}
		
		Player player = (Player) sender;
		
		// checking if the player has the permission to run the specified command
		if (!player.hasPermission("hungergames.commands.arena")) {
			player.sendMessage(Language.NO_PERMISSION.toString());
			return true;
		}
		
		// checking to see if the player supplied any subcommands.. if they didn't, reprimand them for it
		if (args.length < 1) {
			this.printHelpMessage(player);
			return true;
		}
		
		if (args[0].equalsIgnoreCase("list")) {
			
			if (adminCmd.inArenaMode(player)) {
				player.sendMessage(Language.ALREADY_IN_CREATION_MODE.toString());
				return true;
			}
			
			if (!player.hasPermission("hungergames.commands.arena.list")) {
				player.sendMessage(Language.NO_PERMISSION.toString());
				return true;
			}
			
			new ArenaListSubcommand().listArenasToPlayer(player);
			
			return true;
			
		} else if (args[0].equalsIgnoreCase("join")) {
			
			if (adminCmd.inArenaMode(player)) {
				player.sendMessage(Language.ALREADY_IN_CREATION_MODE.toString());
				return true;
			}
			
			if (!player.hasPermission("hungergames.commands.arena.join")) {
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
			
			// checking to see if the player is in a game or not
			if (SurvivalGames.getArenaData().playerInArena(player, false) != null) {
				// stopping the player from joining if they are in game another game
				player.sendMessage(Language.ALREADY_IN_ARENA.toString());
				return true;
			}
			
			new ArenaJoinSubCommand().addPlayerToArena(player, args[1]);
			
			return true;
			
		} else if (args[0].equalsIgnoreCase("leave")) {
			
			if (adminCmd.inArenaMode(player)) {
				player.sendMessage(Language.ALREADY_IN_CREATION_MODE.toString());
				return true;
			}
			
			if (!player.hasPermission("hungergames.commands.arena.leave")) {
				player.sendMessage(Language.NO_PERMISSION.toString());
				return true;
			}
			
			new ArenaLeaveSubCommand().removePlayer(player);
			
			return true;
			
			
		} else if (args[0].equalsIgnoreCase("spectate")) {
			
			// checking if the server even allows spectators
			if (Values.ALLOW_SPECTATORS == false) {
				// run this because spectators aren't allowed
				player.sendMessage(Language.SPECTATING_DISABLED.toString());
				return true;
			}
			
			if (!player.hasPermission("hungergames.commands.arena.spectate")) {
				player.sendMessage(Language.NO_PERMISSION.toString());
				return true;
			}
			
			if (adminCmd.inArenaMode(player)) {
				player.sendMessage(Language.ALREADY_IN_CREATION_MODE.toString());
				return true;
			}
			
			// checking to see if the player is in a game or not
			if (SurvivalGames.getArenaData().playerInArena(player, false) != null) {
				// stopping the player from spectating if they are in game
				player.sendMessage(Language.IN_ARENA_CANT_SPECTATE.toString());
				return true;
			}
			
			String arenaName = SurvivalGames.getArenaData().isSpectating(player);
			
			if (arenaName != null) {
				
				// this will run if the player is already spectating, so we will handle taking them out of that now!
				if (SurvivalGames.getArenaData().removeSpectator(arenaName, player)) {
					
					player.sendMessage(Language.SUCESSFULLY_REMOVED.toString());
					
					player.teleport(SurvivalGames.getArenaData().getArenaLobby(arenaName));
					player.setGameMode(GameMode.SURVIVAL);
					SurvivalGames.getArenaData().removeSpectator(arenaName, player);
					
				} else {
					
					player.sendMessage(Language.UNSUCESSFULLY_REMOVED.toString());
					
				}
				
			} else {
				
				// this will run if the player is not spectating, so wei will add them now!
				
				// check to see if they gave us an arena name!
				if (args.length < 2) {
					player.sendMessage(Language.NO_ARENA_SPECIFIED.toString());
					return true;
				}
				
				if (!SurvivalGames.getArenaData().containsArena(args[1])) {
					player.sendMessage(Language.ARENA_NOT_FOUND.toString());
					return true;
				}
				
				String joiningArenaName = args[1];
				
				if (!SurvivalGames.getArenaData().isArenaActive(joiningArenaName)) {
					player.sendMessage(Language.UNACTIVE_ARENA_FAILED_SPECTATION.toString());
					return true;
				}
				
				// checking to see if the arena is at the spectator limit
				if (SurvivalGames.getArenaData().getSpectatorAmount(joiningArenaName) >= Values.SPECTATOR_LIMIT && Values.SPECTATOR_LIMIT > -1) {
					
					player.sendMessage(Language.AT_SPECTATOR_LIMIT.toString());
					return true;
					
				}
				
				SurvivalGames.getArenaData().addSpectator(joiningArenaName, player);
				SurvivalGames.getArenaData().movePlayerToLobby(joiningArenaName, player);
				
				// set the player's gamemode to spectator
				player.setGameMode(GameMode.SPECTATOR);
				
				player.sendMessage(ChatColor.GREEN + "You have been added as a spectator to the arena: " + joiningArenaName);
				player.sendMessage(Language.ADDED_AS_SPECTATOR.toString(joiningArenaName, null, null, null));
				
			}
			
			return true;
			
		} else {
			
			// An unknown subcommand was given, so just print out the list
			this.printHelpMessage(player);
			return true;
			
		}
		
		
	}
	

	/** Prints a help message to the specified
	 * @param player The player that needs the help message
	 */
	private void printHelpMessage(Player player) {
		
		// compile a list of permissions that the player is able to use:
		List<String> availableCommands = new ArrayList<String>();
		
		if (player.hasPermission("hungergames.commands.arena.list")) {
			availableCommands.add(ChatColor.GOLD + "/hgarena " + ChatColor.RED + "list " + ChatColor.GRAY + "-" + ChatColor.GOLD + " lists all the loaded arenas");
		}
		
		if (player.hasPermission("hungergames.commands.arena.join")) {
			availableCommands.add(ChatColor.GOLD + "/hgarena " + ChatColor.RED + "join <name> " + ChatColor.GRAY + "-" + ChatColor.GOLD + " Joins the arena is possible!");
		}
		
		if (player.hasPermission("hungergames.commands.arena.leave")) {
			availableCommands.add(ChatColor.GOLD + "/hgarena " + ChatColor.RED + "leave " + ChatColor.GRAY + "-" + ChatColor.GOLD + " Leaves the current game!");
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
	
}
