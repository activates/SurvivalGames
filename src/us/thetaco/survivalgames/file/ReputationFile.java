package us.thetaco.survivalgames.file;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;

public class ReputationFile {

	private FileConfiguration customConfig = null;
    private File customConfigFile = null;
    
    /*
     * This class will accept the player's names to reload the config file
     */
    public void reloadCustomConfig() {
        if (customConfigFile == null) {
        customConfigFile = new File(SurvivalGames.getPluginName() + "/reputations.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }
    /*
     * END reload custom config file
     */
   
    /*
     * BEGIN getCustomConfig
     */
    public FileConfiguration getCustomConfig() {
        if (customConfig == null) {
            this.reloadCustomConfig();
        }
        return customConfig;
    }
    /*
     * END custom config
     */

    /*
     * BEGIN saveCustomConfig
     */
    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
        return;
        }
        try {
            getCustomConfig().save(customConfigFile);
            this.reloadCustomConfig();
        } catch (IOException ex) {
            SurvivalGames.logger().logMessage(Language.ERROR_SAVING_REPUTATION.toString());
        }
    }
	
}
