package us.thetaco.survivalgames.commands;

import org.bukkit.entity.Player;

import us.thetaco.survivalgames.file.ArenaFile;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.SurvivalGames;

/** A class for handling the deletion of an arena when the player runs the delete subcommand
 * @author activates
 *
 */
public class ArenaDeleteSubcommand {

	/** Notifies the specified player of what happened when they ran the delete subcommand
	 * @param player The sender of the delete subcommand
	 * @param arenaName The name of the arena to be deleted
	 * @param plugin The plugin's main class (SurvivalGames)
	 */
	public void deleteArena(Player player, String arenaName, SurvivalGames plugin) {
		
		if (!SurvivalGames.getArenaData().removeArena(arenaName)) {
			player.sendMessage(Language.ARENA_NOT_FOUND.toString());
			return;
		}
		
		new ArenaFile(plugin.getDataFolder()).removeArenaFile(arenaName);
		
		player.sendMessage(Language.ARENA_DELETED.toString(arenaName, null, null, null));
		
	}
	
}
