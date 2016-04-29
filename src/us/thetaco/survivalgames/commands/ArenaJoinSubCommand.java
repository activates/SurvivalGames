package us.thetaco.survivalgames.commands;

import org.bukkit.entity.Player;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.Values;

/** A class for handling the outcome of the /hgarena join command. It checks if the player can join and if it needs to start up the lobby
 * or add the player to an already existing one
 * @author activates
 *
 */
public class ArenaJoinSubCommand {

	/** Used in adding a player to an arena. If you want to emulate a player using the join subcommand, this is the method to use.
	 * @param p The player to add to the arena if possible
	 * @param arenaName The arena to add the player to
	 */
	public void addPlayerToArena(Player p, String arenaName) {
		
		if (SurvivalGames.getArenaData().playerInArena(p, false) != null) {
			p.sendMessage(Language.ALREADY_IN_ARENA.toString());
			return;
		}
		
		String newArenaName = arenaName.toLowerCase();
		
		// check to make sure the player has the required reputation to participate in this arena
		if (Values.REPUTATION_ENABLED && SurvivalGames.getArenaData().getReputation(p) < SurvivalGames.getArenaData().getMinimumRep(newArenaName)) {
			// this will run if the player cannot join the arena due to their reputation
			p.sendMessage(Language.REPUTATION_TOO_LOW.toString());
			
			return;
		}
		
		// we also need to check and see if the game has even been started, yet..
		if (!SurvivalGames.getArenaData().isArenaActive(newArenaName) && !SurvivalGames.getLobbyHandler().activeLobby(newArenaName)) {
			
			// if this runs.. we know the game isn't active and neither is the lobby, so we'll start it off
			SurvivalGames.getLobbyHandler().startLobbyTimer(newArenaName, SurvivalGames.getArenaData().getLobbyTime(newArenaName));
			
			
		}
		
		// check to make sure the player can actually join the arena
		if (SurvivalGames.getArenaData().isArenaActive(newArenaName)) {
			
			// if this runs, that means the player cannot join because the arena is already active
			p.sendMessage(Language.ARENA_NOT_JOINABLE.toString(arenaName, null, null, null));
			return;
			
		}

		// we'll also check if the lobby is full, too
		
		if (SurvivalGames.getLobbyHandler().isFull(newArenaName)) {
			
			// notify the player that the arena is full and they cannot join because of this
			p.sendMessage(Language.ARENA_FULL.toString(newArenaName, null, null, null));
			return;
			
		}
		
		// if all is we'll.. add the player to the lobby
		
		
		p.sendMessage(Language.ADDED_TO_ARENA.toString());
		SurvivalGames.getLobbyHandler().addPlayerToLobby(arenaName, p);
		
		SurvivalGames.getArenaData().movePlayerToLobby(newArenaName, p);
		
		// message all the players that the specified player has joined the arena
		SurvivalGames.getLobbyHandler().messagePlayersInLobby(arenaName, Language.JOIN_BROADCAST.toString(null, p, null, null));
		
	}
	
}
