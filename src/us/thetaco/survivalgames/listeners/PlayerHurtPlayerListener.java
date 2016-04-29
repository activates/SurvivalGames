package us.thetaco.survivalgames.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import us.thetaco.survivalgames.SurvivalGames;

/** A class for managing the prevention of PvP for players who are in a lobby
 * @author activates
 *
 */
public class PlayerHurtPlayerListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDamage(EntityDamageByEntityEvent event) {
		
		/*
		 * We will employ a method of checking if the attacker is a player.. if the attacker is in a lobby
		 * and they are attacking another player.. cancel the event. Simple as that
		 */
		
		// checking to make sure the event isn't already cancelled
		if (event.isCancelled()) return;
		
		// not going any further if the attacker is not a player
		if (event.getDamager().getType() != EntityType.PLAYER) return;
		
		// if the attacked is not a player, don't go any further
		if (event.getEntity().getType() != EntityType.PLAYER) return;
		
		// if we get to this point, we need to cancel the event because we know the attacker and the attacked are both players
		// we still need to check if the attacker is in a lobby... however
		
		Player attacker = (Player) event.getDamager();
		Player attacked = (Player) event.getEntity();
		
		String lobbyNameAttacker = SurvivalGames.getLobbyHandler().getPlayerArenaName(attacker);
		String lobbyNameAttacked = SurvivalGames.getLobbyHandler().getPlayerArenaName(attacked);
		
		/*
		 * Basically, if anyone is involed in getting attacked by another player whilst they are in a lobby..
		 * it will be cancelled
		 */
		
		if (lobbyNameAttacked != null) {
			event.setCancelled(true);
		}
		
		if (lobbyNameAttacker != null) {
			event.setCancelled(true);
		}
		
	}
	
}
