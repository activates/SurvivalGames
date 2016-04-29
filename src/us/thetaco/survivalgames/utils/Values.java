package us.thetaco.survivalgames.utils;

import java.util.ArrayList;
import java.util.List;

/** Holds all the config values
 * @author activates
 *
 */
public abstract class Values {

	/** Used to determine if the no point of "return option is enabled"
	 * 
	 */
	public static boolean USE_NO_RETURN = true;
	
	
	/**  Used if the USE_NO_RETURN option is enabled... It's the amount in seconds where after that point
	 * the game cannot be ended until only 1 player is left (or the time runs out)
	 * 
	 */
	public static int NO_RETURN_SECONDS = 300;
	
	/** Used to determine if spectators should be allowed or not
	 * 
	 */
	public static boolean ALLOW_SPECTATORS = true;
	
	/** The desired spectator limit (only useful if ALLOW_SPECTATORS is true)
	 * 
	 */
	public static int SPECTATOR_LIMIT = -1;
	
	/** Used to determine if all commands are allowed in an arena game
	 * 
	 */
	public static boolean ALLOW_COMMANDS = false;
	
	/** The commands that are allowed even if ALLOW_COMMANDS is set to false
	 * 
	 */
	public static List<String> BYPASSING_COMMANDS = new ArrayList<String>();
	
	/** Used to determine if players are struck by lighting when they die
	 * 
	 */
	public static boolean LIGHTNING_ENABLED = true;

	/** Used to determine if the reputation system should be used or not
	 * 
	 */
	public static boolean REPUTATION_ENABLED = true;
	
	/** Used to determine the default reputation (only useful if reputation is enabled)
	 * 
	 */
	public static int DEFAULT_REPUTATION = 0;
	
	/** Used to determine the minimum amount of seconds a player must be in an arena before they will earn a reputation point.
	 * If they disconnect from the game, they will recieve a negative rep point not matter how long they were in the game
	 * 
	 */
	public static int MINIMUM_REP_ADD = 120;
	
	/** Used to determine if other player's an arena will be notified that a player disconnecting during mid game has negatively
	 * impacted their reputation
	 * 
	 */
	public static boolean BROADCAST_MINUS_REP = true;
	
	/** Used to determine if the border system will be used in arenas
	 * 
	 */
	public static boolean BORDER_ENABLED = true;
	
	/** Used to determine how many arena ticks it will take to shrink the border by one block
	 * 
	 */
	public static int BORDER_TICK_DELAY = 40;
	
	/** Used to determine how small the border can get in an arena before it stops shrinking
	 * 
	 */
	public static int BORDER_FLOOR = 50;
	
	/** Used to determine how much the border closes in each specified interval
	 * 
	 */
	public static double BOREDER_DECREMENT_AMOUNT = 1.0;
	
}
