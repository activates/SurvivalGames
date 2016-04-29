package us.thetaco.survivalgames;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import us.thetaco.survivalgames.clockwork.ArenaTimer;
import us.thetaco.survivalgames.clockwork.LobbyTimer;
import us.thetaco.survivalgames.commands.AdminCommand;
import us.thetaco.survivalgames.commands.ArenaCommand;
import us.thetaco.survivalgames.file.ArenaFile;
import us.thetaco.survivalgames.file.ConfigHandler;
import us.thetaco.survivalgames.file.ReputationFile;
import us.thetaco.survivalgames.listeners.PlayerChatListener;
import us.thetaco.survivalgames.listeners.PlayerCommandListener;
import us.thetaco.survivalgames.listeners.PlayerDeathListener;
import us.thetaco.survivalgames.listeners.PlayerDisconnectListener;
import us.thetaco.survivalgames.listeners.PlayerHurtPlayerListener;
import us.thetaco.survivalgames.listeners.PlayerMovementListener;
import us.thetaco.survivalgames.listeners.PlayerRespawnListener;
import us.thetaco.survivalgames.utils.ArenaData;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.SimpleLogger;

/** The main class of the Survival-Games plugin for the server Admixia
 * This plugin is subject to the General Public License version 3 (GPLv3)
 * 
 * @author activates
 * 
 */
public class SurvivalGames extends JavaPlugin {

	// Creating new objects of classes here to pass to other Classes
	private ArenaFile aFile = new ArenaFile(this.getDataFolder());
	private AdminCommand adminCommand;
	private ArenaCommand arenaCommand;
	private PlayerChatListener pChatListener;
	private PlayerDisconnectListener pDisconnectListener;
	private PlayerMovementListener pMoveListener = new PlayerMovementListener();
	private PlayerHurtPlayerListener pHurtListener = new PlayerHurtPlayerListener();
	private PlayerRespawnListener pRespawnListener = new PlayerRespawnListener();
	private PlayerCommandListener pCommandListener = new PlayerCommandListener();
	private PlayerDeathListener pDeathListener;
	private static ArenaData arenaData;
	private static LobbyTimer lobbyHandler;
	private static ArenaTimer arenaHandler;
	private static String pluginName;
	private ConfigHandler configHandler;
    private static YamlConfiguration LANG;
    private static File LANG_FILE;
    private static ReputationFile repFile;
	
	public void onEnable() {
		
		// plugin name first!
		SurvivalGames.pluginName = this.getName();
		
		// loading the language file
		this.loadLang();
		
		if (!this.getServer().getPluginManager().isPluginEnabled(this)) {
			return;
		}
		
		// initializing the required variables
		SurvivalGames.arenaData = new ArenaData();
		SurvivalGames.lobbyHandler = new LobbyTimer(this);
		SurvivalGames.arenaHandler = new ArenaTimer(this);
		this.configHandler = new ConfigHandler(this);
		this.adminCommand = new AdminCommand(this, configHandler);
		this.arenaCommand = new ArenaCommand(adminCommand);
		SurvivalGames.repFile = new ReputationFile();
		
		// loading the config values
		configHandler.initializeConfig();
		
		// registering 'special' listeners
		pDisconnectListener = new PlayerDisconnectListener(adminCommand, pChatListener);
		pDeathListener = new PlayerDeathListener(pRespawnListener);
		pChatListener = new PlayerChatListener(adminCommand, aFile);
		
		// loading all of the arenas
		this.loadArenas();
		
		// registering commands and events
		this.registerCommands();
		this.registerEvents();
		
	}
	
	public void onDisable() {
		
		// clearing all static data, so it can be reassigned
		SurvivalGames.arenaData = null;
		SurvivalGames.lobbyHandler = null;
		SurvivalGames.arenaHandler = null;
		this.configHandler = null;
		this.adminCommand = null;
		this.arenaCommand = null;
		SurvivalGames.repFile = null;
		
	}
	
	/**
	 * @return Returns the logger for this plugin. The Conditions for it are under the Enum Conditions
	 */
	public static SimpleLogger logger() {
		return new SimpleLogger();
	}
	
	/** Does exactly what the method-name entails.. it registers all the commands
	 * 
	 */
	private void registerCommands() {
		
		this.getCommand("hgarena").setExecutor(arenaCommand);
		this.getCommand("hgadmin").setExecutor(adminCommand);
		
	}
	
	/** Also fairly easy to understand.. it just registers the events in a nice and easy manner
	 * 
	 */
	private void registerEvents() {
		
		PluginManager pm = this.getServer().getPluginManager();
		
		pm.registerEvents(pChatListener, this);
		pm.registerEvents(pDisconnectListener, this);
		pm.registerEvents(pDeathListener, this);
		pm.registerEvents(pMoveListener, this);
		pm.registerEvents(pHurtListener, this);
		pm.registerEvents(pRespawnListener, this);
		pm.registerEvents(pCommandListener, this);
		
	}
	
	/** Used for fetching the arena data
	 * @return The class containing all the arena data
	 */
	public static ArenaData getArenaData() {
		
		return SurvivalGames.arenaData;
		
	}
	
	/** Used for fetching the lobby handler
	 * @return The class for handling the lobby in games
	 */
	public static LobbyTimer getLobbyHandler() {
		
		return SurvivalGames.lobbyHandler;
		
	}
	
	/** Used for fetching the arena handler
	 * @return The class for handling the timings in the arena games
	 */
	public static ArenaTimer getArenaHandler() {
		
		return SurvivalGames.arenaHandler;
		
	}
	
	/** Used to return the name of the plugin
	 * @return
	 */
	public static String getPluginName() {
		
		return SurvivalGames.pluginName;
		
	}
	
	/** A method that loads all the arenas in the filesystem into memory
	 * 
	 */
	private void loadArenas() {
		
		List<String> loadedArenas = aFile.getArenas();
		
		// if there are no arenas, yet.. we don't need to load any
		if (loadedArenas == null) return;
		
		if (loadedArenas == null || loadedArenas.size() < 1) return;
		
		SurvivalGames.arenaData.setArenas(loadedArenas);

		for (String s : SurvivalGames.arenaData.listArenas()) {
			
			List<String> arenaSpawns = aFile.getArenaSpawns(s);
			
			boolean worldExist = false;
			
			if (arenaSpawns != null) {
				
				List<String> spawnPoints = aFile.getArenaSpawns(s);
				
				for (String ss : spawnPoints) {
					
					String[] deformatted = ss.split("//");
					
					if (deformatted[0] != null && Bukkit.getWorld(deformatted[0]) != null) {
						
						worldExist = true;
						
					}
					
				}
				
				if (worldExist) {
				
					SurvivalGames.arenaData.setSpawnPoints(s, aFile.getArenaSpawns(s));
				
				}
				
			}
						
			if (worldExist) {
				
				SurvivalGames.arenaData.setMinimumPlayerCount(s, aFile.getMPCount(s));
				SurvivalGames.arenaData.setArenaLobby(s, aFile.getLobbyLocation(s));
				SurvivalGames.arenaData.setLobbyTime(s, aFile.getLobbyTime(s));
				SurvivalGames.arenaData.setArenaTime(s, aFile.getArenaTime(s));
				SurvivalGames.arenaData.setMinimumRep(s, aFile.getMinimumRep(s));
				SurvivalGames.arenaData.setCenterLocation(s, aFile.getCenterLocation(s));
				SurvivalGames.arenaData.setDefaultBorderRadius(s, aFile.getArenaRadius(s));
				
			}
			
			if (!worldExist) {
				
				SurvivalGames.logger().logMessage("The world for arena " + s + " doesn't exist!");
				
			}
			
		}
		
		
	}
	
	/** Used to load all the language values from the language.yml file. If none exists or some values are missing, this will
	 * automatically set them to whatever the default is
	 * 
	 */
	public void loadLang() {
		
		File dir = new File(SurvivalGames.getPluginName());
		
		// creating the directory if it doens't exist
		if (!dir.exists()) dir.mkdir();
		
	    File lang = new File(SurvivalGames.getPluginName() + "/language.yml");
	    if (!lang.exists()) {
	        try {
	            getDataFolder().mkdir();
	            lang.createNewFile();
	            InputStream defConfigStream = this.getResource(SurvivalGames.getPluginName() + "/language.yml");
	            if (defConfigStream != null) {
	                @SuppressWarnings("deprecation")
					YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	                defConfig.save(lang);
	                Language.setFile(defConfig);
	                return;
	            }
	        } catch(IOException e) {
	            e.printStackTrace(); // So they notice
	            SurvivalGames.logger().logMessage("Couldn't create language file");
	            SurvivalGames.logger().logMessage("Without the language file this plugin doesn't know what to say!");
	            this.setEnabled(false);
	        }
	    }
	    YamlConfiguration conf = YamlConfiguration.loadConfiguration(lang);
	    for(Language item:Language.values()) {
	        if (conf.getString(item.getPath()) == null) {
	            conf.set(item.getPath(), item.getDefault());
	        }
	    }
	    Language.setFile(conf);
	    SurvivalGames.LANG = conf;
	    SurvivalGames.LANG_FILE = lang;
	    try {
	        conf.save(getLangFile());
	    } catch(IOException e) {
	        SurvivalGames.logger().logMessage("Not able to create and save a new language file. Reason: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	/** Used to get the parsed YAML from the language file
	 * @return The YamlConfiguration of the language file
	 */
	public YamlConfiguration getLang() {
	    return LANG;
	}
	 
	/**
	* Get the language.yml file.
	* @return The language.yml file.
	*/
	public File getLangFile() {
	    return LANG_FILE;
	}
	
	/** Used to get the reputation file which holds all the player reputation data in YAML form
	 * @return The ReputationFile class for saving, writing, and reloading the config
	 */
	public static ReputationFile getRepConfig() {
		
		return SurvivalGames.repFile;
		
	}
	
}
