package us.thetaco.survivalgames.commands;

import org.bukkit.entity.Player;

import us.thetaco.survivalgames.utils.Language;

/** A class simply for dealing with a player who ran the /arena create subcommand.
 * Not much will be happening here except the sending of messages
 * @author activates
 *
 */
public class ArenaCreateSubCommand {
	
	/** Notifies the player that they have been put into the arena-creation mode
	 * @param player The player to who will be setup to create an arena
	 */
	public void setupToCreate(Player player) {
		
		player.sendMessage(Language.ARENA_CREATE_START_MESSAGE.toString());
		
		
		// TODO: fancy up a lot
		
	}
	
}
