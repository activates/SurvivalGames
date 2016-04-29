package us.thetaco.survivalgames.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

public class SimpleLogger {

public enum Condition { WARNING, ERROR, NORMAL };
	
	public void logMessage(String message, Enum<Condition> condition) {
		
		Logger logger = Bukkit.getLogger();
		
		if (condition == Condition.WARNING) {
			logger.setLevel(Level.WARNING);
		} else if (condition == Condition.ERROR) {
			logger.setLevel(Level.SEVERE);
		} else {
			logger.setLevel(Level.INFO);
		}
		
		logger.info("[SG] " + message);
		
	}
	
	public void logMessage(String message) {
		
		Bukkit.getLogger().info("[SG] " + message);
		
	}
	
}
