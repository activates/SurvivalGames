package us.thetaco.survivalgames.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;
import us.thetaco.survivalgames.utils.SimpleLogger.Condition;

/** A class entirely for working with arena config management.
 * All configs will be serialized, but active arenas will be loaded into RAM
 * 
 * @author activates
 * 
 */
public class ArenaFile {

	private File pluginDirectory;
	public ArenaFile(File pluginDirectory) {
		this.pluginDirectory = pluginDirectory;
	}
	
	/** A method that creates the new arena file
	 * @param arenaName The name of the arena to create
	 * @param spawnCoordinates The spawns coordinates for the specified arena
	 * @param mpCount the minimum player count for the specified arena
	 */
	public void createArena(String arenaName, List<String> spawnCoordinates, int mpCount, Location lobby, int lobbyTime, int arenaTime, int minimumRep, String centerLocation, int arenaRadius) {
		
		// doing to simple check to make sure all data is present..
		// the arena name will be checked in the command
		if (arenaName == null || spawnCoordinates == null || spawnCoordinates.size() == 0 || lobby == null) return;
		
		// Checking to make sure the arena directory exists
		this.createDirectory();
		
		// serializing the data before writing it to the file
		String serializedData = arenaName + " ++ ";
		
		// serializing all the data from the spawnCoordinates
		String spawnCoords = "";
		
		if (spawnCoordinates.size() < 2) {
			
			// if there is only one entry in the spawn coordinates, we will just add it here
			spawnCoords = spawnCoordinates.get(0);
			
		} else {
		
			// if there is more than one entry in the spawn coordinates, we will input it here
			
			spawnCoords = spawnCoordinates.get(0);
			
			int i = 0;
			for (String s : spawnCoordinates) {
				if (i > 0) spawnCoords += ".." + s;
				i++;
			}
			
		}
		
		// compile the entirety of the serialized data:
		serializedData += spawnCoords;
		// adding the minimum player count
		serializedData += " ++ " + mpCount;
		
		// serializing the lobby location
		String serializedLobby = ArenaFile.formatString(lobby.getWorld(), lobby.getX(), lobby.getY(), lobby.getZ(), lobby.getYaw(), lobby.getPitch());
		
		// adding it to the serialized data
		serializedData += " ++ " + serializedLobby + " ++ " + lobbyTime + " ++ " + arenaTime + " ++ " + minimumRep + " ++ " + centerLocation + " ++ " + arenaRadius;
		
		File arenaFile = new File(this.pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		// passing all this love onto another thread to avoid lag
		Thread t = new Thread(new ArenaFileWriter(arenaFile, serializedData));
		t.start();
		
	}
	
	/** A method for fetching all arenas stored in the filesystem. Only used upon loading the plugin
	 * @return A list of all the arenanames
	 */
	public List<String> getArenas() {
		
		File arenasDirectory = new File(this.pluginDirectory.getName() + "/arenas");
		
		if (!arenasDirectory.exists()) return null;
		
		List<String> arenaNames = new ArrayList<String>();
		
		for (String s : arenasDirectory.list()) {
			
			arenaNames.add(s.replace(".act", ""));
			
		}
		
		return arenaNames;
		
	}
	
	/** Removes an arena fromt the fileSystem if available
	 * @param arenaName The arena that needs to be removed
	 */
	public void removeArenaFile(String arenaName) {
		
		// since this method will be "looping" through files and can lag the thread, we will use a seperate Runnable class
		// that will do the work for us
		
		Thread t = new Thread(new ArenaFileDeleter(arenaName, this.pluginDirectory));
		t.start();
		
	}
	
	/** Formats the data so it can be written to a flat-file
	 * @param world world of the specified coordinates
	 * @param x x spawn coord
	 * @param y y spawn coord
	 * @param z z spawn coord
	 * @param yaw the yaw of the spawn coord
	 * @param pitch the pitch of the spawn coord
	 * @return A String ready to be writen to the database
	 */
	public static String formatString(World world, double x, double y, double z, float yaw, float pitch) {
		
		String formattedString = world.getName() + "//" + x + "//" + y + "//" + z + "//" + yaw + "//" + pitch;
		
		return formattedString;
		
	}
	
	/** Used to unserialize a formatted location for an arena spawnpoint
	 * @param formattedLocation The formatted location.. Should have been formatted with the formatString method
	 * @return Will return the location form of the inputted String. If given an invalid input, the location will return null.
	 */
	public static Location deformatString(String formattedLocation) {
		
		// making sure the inputed string isn't null..
		if (formattedLocation == null) return null;
		
		// breaking the string up
		String[] deformattedLocation = formattedLocation.split("//");
		// and checking to make sure it does have the appropriate parts
		if (deformattedLocation.length < 6) return null;
		
		World specifiedWorld = Bukkit.getWorld(deformattedLocation[0]);
		
		if (specifiedWorld == null) {
			return null;
		}
		
		Double x;
		Double y;
		Double z;
		float yaw;
		float pitch;
		
		try {
			
			x = Double.parseDouble(deformattedLocation[1]);
			y = Double.parseDouble(deformattedLocation[2]);
			z = Double.parseDouble(deformattedLocation[3]);
			yaw = Float.parseFloat(deformattedLocation[4]);
			pitch = Float.parseFloat(deformattedLocation[5]);
			
		} catch (Exception e) {
			
			return null;
		}
		
		Location toReturn = new Location(specifiedWorld, x, y, z, yaw, pitch);
		
		return toReturn;
		
	}
	
	/** A method for fetching arena spawns.. This should only be used for startup because it can lag the main thread (slow hard-drive or something like that)
	 * @param arenaName The arena to fetch spawns for
	 * @return Returns a list of the formatted arena spawns
	 */
	public List<String> getArenaSpawns(String arenaName) {
				
		File arenaFile = new File(pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		// checking to see if the arena actually exists or not..
		if (!arenaFile.exists()) return null;
		
		try {
			
			FileReader fReader = new FileReader(arenaFile);
			BufferedReader reader = new BufferedReader(fReader);
			
			String line = null;
			
			if ((line = reader.readLine()) != null) {
				
				String[] firstSplit = line.split(" \\+\\+ ");
				String[] secondSplit = firstSplit[1].split("\\.\\.");
				
				List<String> toReturn = new ArrayList<String>();
				
				for (String s : secondSplit) {
					
					toReturn.add(s);
					
				}
				
				reader.close();
				return toReturn;
				
			}
			
			reader.close();
			return null;
			
		} catch (IOException e) {

			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString());
			return null;

		} 
		
		
		
	}
	
	/** Used to retrieve the stored Minimum Player counts. Should only be used on startup
	 * @param arenaName The name of the arena in question
	 * @return The number stored for the minium player count. Will return 1 if one wasn't set for some reason..
	 */
	public int getMPCount(String arenaName) {
		
		File arenaFile = new File(pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		// checking to see if the arena actually exists or not..
		if (!arenaFile.exists()) return 1;
		
		try {
			
			FileReader fReader = new FileReader(arenaFile);
			BufferedReader reader = new BufferedReader(fReader);
			
			String line = null;
			
			if ((line = reader.readLine()) != null) {
				
				String[] firstSplit = line.split(" \\+\\+ ");
				
				int mpCount = 0;
				
				try {
					
					mpCount = Integer.parseInt(firstSplit[2]);
					
				} catch (Exception ex) {
					ex.printStackTrace();
					SurvivalGames.logger().logMessage(Language.MP_COUNT_NOT_FOUND.toString());
					reader.close();
					return 1;
				}
				
				reader.close();
				return mpCount;
				
			}
			
			reader.close();
			return 1;
		} catch (IOException e) {

			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.ERROR);
			return 1;

		} 
		
	}
	
	/** Used to get the Arena's lobby location
	 * @param arenaName The arena to get the lobby for
	 * @return Will return null if the lobby doesn't exist.. Otherwise it will return the formatted coordinates. Use the deformatter in this class
	 * to un-serialize it.
	 */
	public String getLobbyLocation(String arenaName) {
		
		File arenaFile = new File(pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		if (!arenaFile.exists()) return null;
		
		try {
			
			FileReader fReader = new FileReader(arenaFile);
			BufferedReader reader = new BufferedReader(fReader);
			
			String line = null;
			
			if ((line = reader.readLine()) != null) {
				
				String[] firstSplit = line.split(" \\+\\+ ");
				
				// return null because the arena format is not formatted correctly
				if (firstSplit.length < 4) {
					reader.close();
					return null;
				}
				
				String formattedLocation = firstSplit[3];
				
				reader.close();
				
				if (formattedLocation.split("//").length < 6) {
					System.out.println(formattedLocation);
					return null;
				} else {
					return formattedLocation;
				}
				
				
			}
			
			reader.close();
			return null;
		} catch (IOException e) {

			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.ERROR);
			return null;

		}
		
	}
	
	/** Used to get the Arena's lobby time
	 * @param arenaName The arena to get the lobby time for
	 * @return Will return null if the arena doesn't exist.. Otherwise it will return the lobby time in seconds.
	 */
	public Integer getLobbyTime(String arenaName) {
		
		File arenaFile = new File(pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		if (!arenaFile.exists()) return null;
		
		try {
			
			FileReader fReader = new FileReader(arenaFile);
			BufferedReader reader = new BufferedReader(fReader);
			
			String line = null;
			
			if ((line = reader.readLine()) != null) {
				
				String[] firstSplit = line.split(" \\+\\+ ");
				
				// return null because the arena format is not formatted correctly
				if (firstSplit.length < 5) {
					reader.close();
					return null;
				}
				
				String timeSplit = firstSplit[4];
				
				reader.close();
				
				int lobbyTime = Integer.parseInt(timeSplit);
				
				return lobbyTime;
			}
			
			reader.close();
			return null;
		} catch (Exception e) {

			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.ERROR);
			return null;

		}
		
	}
	
	/** Used to get the Arena's lobby time
	 * @param arenaName The arena to get the lobby time for
	 * @return Will return null if the arena doesn't exist.. Otherwise it will return the lobby time in seconds.
	 */
	public Integer getArenaTime(String arenaName) {
		
		File arenaFile = new File(pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		if (!arenaFile.exists()) return null;
		
		try {
			
			FileReader fReader = new FileReader(arenaFile);
			BufferedReader reader = new BufferedReader(fReader);
			
			String line = null;
			
			if ((line = reader.readLine()) != null) {
				
				String[] firstSplit = line.split(" \\+\\+ ");
				
				// return null because the arena format is not formatted correctly
				if (firstSplit.length < 6) {
					reader.close();
					return null;
				}
				
				String timeSplit = firstSplit[5];
				
				reader.close();
				
				int arenaTime = Integer.parseInt(timeSplit);
				
				return arenaTime;
			}
			
			reader.close();
			return null;
		} catch (Exception e) {

			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.ERROR);
			return null;

		}
		
	}
	
	/** Used to get the Arena's lobby time
	 * @param arenaName The arena to get the lobby time for
	 * @return Will return null if the arena doesn't exist.. Otherwise it will return the lobby time in seconds.
	 */
	public Integer getMinimumRep(String arenaName) {
		
		File arenaFile = new File(pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		if (!arenaFile.exists()) return null;
		
		try {
			
			FileReader fReader = new FileReader(arenaFile);
			BufferedReader reader = new BufferedReader(fReader);
			
			String line = null;
			
			if ((line = reader.readLine()) != null) {
				
				String[] firstSplit = line.split(" \\+\\+ ");
				
				// return null because the arena format is not formatted correctly
				if (firstSplit.length < 7) {
					reader.close();
					return null;
				}
				
				String timeSplit = firstSplit[6];
				
				reader.close();
				
				int arenaTime = Integer.parseInt(timeSplit);
				
				return arenaTime;
			}
			
			reader.close();
			return null;
		} catch (Exception e) {

			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.ERROR);
			return null;

		}
		
	}
	
	/** Used to get the Arena's formatted center location
	 * @param arenaName The arena to get the center location for
	 * @return Will return null if the arena doesn't exist.. Otherwise it will return the center location in string form
	 */
	public String getCenterLocation(String arenaName) {
		
		File arenaFile = new File(pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		if (!arenaFile.exists()) return null;
		
		try {
			
			FileReader fReader = new FileReader(arenaFile);
			BufferedReader reader = new BufferedReader(fReader);
			
			String line = null;
			
			if ((line = reader.readLine()) != null) {
				
				String[] firstSplit = line.split(" \\+\\+ ");
				
				// return null because the arena format is not formatted correctly
				if (firstSplit.length < 8) {
					reader.close();
					return null;
				}
				
				String locSplit = firstSplit[7];
				
				reader.close();
				
				return locSplit;
			}
			
			reader.close();
			return null;
		} catch (Exception e) {

			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.ERROR);
			return null;

		}
		
	}
	
	/** Used to get the Arena's radius size
	 * @param arenaName The arena to get the radius for
	 * @return Will return 0 if the arena doesn't exist.. Otherwise it will return the radius size of the specified arena
	 */
	public int getArenaRadius(String arenaName) {
		
		File arenaFile = new File(pluginDirectory.getName() + "/arenas/" + arenaName + ".act");
		
		if (!arenaFile.exists()) return 0;
		
		try {
			
			FileReader fReader = new FileReader(arenaFile);
			BufferedReader reader = new BufferedReader(fReader);
			
			String line = null;
			
			if ((line = reader.readLine()) != null) {
				
				String[] firstSplit = line.split(" \\+\\+ ");
				
				// return null because the arena format is not formatted correctly
				if (firstSplit.length < 9) {
					reader.close();
					return 0;
				}
				
				String locSplit = firstSplit[8];
				
				reader.close();
				
				return Integer.parseInt(locSplit);
			}
			
			reader.close();
			return 0;
		} catch (Exception e) {

			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.NORMAL);
			return 0;

		}
		
	}
	
	/** Creates the directory that all arenas are stored in if it doesn't exist
	 * @return If the directory could be created successfully or not (will return true if the directory already exists)
	 */
	private boolean createDirectory() {
		
		File arenaDirectory = new File(pluginDirectory.getName() + "/arenas");
		
		try {
			
			if (arenaDirectory.exists()) return true;
			
			arenaDirectory.mkdirs();
			arenaDirectory.mkdir();
			
			return true;
			
		} catch (Exception e) {
			SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.ERROR);
			return false;
		}
		
	}
	
	/** A private class just for writing to arena files. It is done in a seperate thread
	 * @author activates
	 *
	 */
	private class ArenaFileWriter implements Runnable {
		
		private File toEdit;
		private String lineToAdd;
		
		public ArenaFileWriter(File toEdit, String lineToAdd) {
			this.toEdit = toEdit;
			this.lineToAdd = lineToAdd;
		}
		
		@Override
		public void run() {
			
			// all stuff will be ran in here (exciting I know)
			
			// we won't have to worry about keeping the old lines, but instead we can just write over the old data in the file

			try {
				
				// setting up everything to write to the file
				FileWriter fWriter = new FileWriter(toEdit);
				BufferedWriter writer = new BufferedWriter(fWriter);
				
				// writing to the file
				writer.write(lineToAdd);
				writer.newLine();
				writer.write(Language.NEW_CONFIG_GENERATED.toString());
				
				// tidying things up
				writer.close();
				fWriter.close();
				
				List<String> spawns = new ArrayList<String>();
				
				/*
				 * Looks like I loaded some of the arena data right in the cache... not sure why I did this, but not
				 * in the chat listener.. I will need to change that
				 * TODO: Move all this stuff to the PlayerChatListener for organization's sake
				 */
				
				String[] nameSplit = lineToAdd.split(" \\+\\+ ");
				String[] spawnSplit = nameSplit[1].split("\\.\\.");
				spawns = Arrays.asList(spawnSplit);
				
				SurvivalGames.logger().logMessage(Language.ARENA_ADDED.toString());
				SurvivalGames.getArenaData().addArena(lineToAdd.split(" ")[0], spawns);
				SurvivalGames.getArenaData().setMinimumPlayerCount(nameSplit[0].toLowerCase(), Integer.parseInt(nameSplit[2]));
				
			} catch (IOException e) {
				
				SurvivalGames.logger().logMessage(Language.FILE_RETRIEVAL_ERROR.toString() + e.getMessage(), Condition.ERROR);
				
			}
			
			
		}
		
	}
	
	private class ArenaFileDeleter implements Runnable {
		
		private String arenaName;
		private File pluginDirectory;
		public ArenaFileDeleter(String arenaName, File pluginDirectory) {
			this.arenaName = arenaName;
			this.pluginDirectory = pluginDirectory;
		}
		
		@Override
		public void run() {
			
			File arenasLocation = new File(pluginDirectory.getName() + "/arenas");
			
			for (String s : arenasLocation.list()) {
				
				String preparedFilename = s.replace(".act", "");
				
				if (preparedFilename.equalsIgnoreCase(arenaName)) {
					
					File toDelete = new File(pluginDirectory.getName() + "/arenas/" + s);
						
					toDelete.delete();
					
				}
				
			}
			
		}
		
	}
	
}
