package us.thetaco.survivalgames.commands;

import java.util.List;

import org.bukkit.entity.Player;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;

/** Handles listing the arenas to the sender of the list subcommand
 * @author activates
 *
 */
public class ArenaListSubcommand {

	/** Lists the loaded arenas to the player
	 * @param p The player who ran the list subcommand
	 */
	public void listArenasToPlayer(Player p) {
		
		// compile the available arenas in one string
		List<String> totalArenas = SurvivalGames.getArenaData().listArenas();
		
		String compiledArenas = "";
		
		if (totalArenas.size() > 1) {
		
			compiledArenas = totalArenas.get(0);
			
			int i = 0;
			for (String s : totalArenas) {
				if (i > 0) compiledArenas += ", " + s;
				i++;
			}
			
		} else if (totalArenas.size() == 1) {
			
			compiledArenas = totalArenas.get(0);
			
		} else {
			
			compiledArenas = "n/a";
			
		}
		
		p.sendMessage(Language.LOADED_ARENAS_HEADER.toString());
		p.sendMessage(compiledArenas);
		
	}
	
}
