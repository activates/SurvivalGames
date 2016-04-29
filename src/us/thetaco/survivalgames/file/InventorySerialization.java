package us.thetaco.survivalgames.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import us.thetaco.survivalgames.SurvivalGames;
import us.thetaco.survivalgames.utils.Language;

public class InventorySerialization {

	private String playerUUID;
	private String inventoryFile;
	public InventorySerialization(String playerUUID) {
		this.playerUUID = playerUUID;
		this.inventoryFile = "SurvivalGames-activates/inventories/" + playerUUID;
	}

	
	/** Used to determine if a player has a serialized inventory in the filesystem
	 * @return Will return true if the player has an inventory stored in the file system
	 */
	public boolean inventorySerialized() {
		
		File inventoryFileDirect = new File(this.inventoryFile + "-base.inv");
		
		if (!inventoryFileDirect.exists()) {
			return false;
		} else {
			return true;
		}
		
	}
	
	/** Used to create the new files for the specified player (needs to be done before you attempt to write at with an object
	 * output stream)
	 * 
	 */
	private void createNewFiles() {
		
		File baseFile = new File(this.inventoryFile + "-base.inv");
		File armorFile = new File(this.inventoryFile + "-armor.inv");
		
		try {
			
			if (!baseFile.exists()) baseFile.createNewFile();
			if (!armorFile.exists()) armorFile.createNewFile();
			
		} catch (IOException e) {
			
			SurvivalGames.logger().logMessage("There was an error while creating inventory storing files for user UUID: " + this.playerUUID);
			e.printStackTrace();
			
		}
		
	}
	
	/** Used to serialize a player's inventory so that it can be restored later
	 * @param playerUUID The uuid to store the inventory under
	 * @param inv The inventory to store
	 */
	public void serializeInventory(PlayerInventory inv) {
		
		if (!this.directoryExists()) this.mkDirs();
		
		// checking and creating a new file if it does not yet exist
		if (!this.inventorySerialized()) {
			this.createNewFiles();
		}
		
		List<InventorySerializer> toWriteInv = new ArrayList<InventorySerializer>();
		List<InventorySerializer> toWriteArm = new ArrayList<InventorySerializer>();
		
		for (ItemStack i : inv.getContents()) {
			
			if (i != null) {
				toWriteInv.add(new InventorySerializer(i));
			}
			
		}
		
		for (ItemStack i : inv.getArmorContents()) {
			
			if (i != null) {
				toWriteArm.add(new InventorySerializer(i));
			}
			
		}
		
		// writing to the file
		try {
			
			FileOutputStream outputStream = new FileOutputStream(this.inventoryFile + "-base.inv");
			ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);
			
			objectOutput.writeObject(toWriteInv);
			
			objectOutput.close();
			outputStream.close();
			
			outputStream = new FileOutputStream(this.inventoryFile + "-armor.inv");
			objectOutput = new ObjectOutputStream(outputStream);
			
			objectOutput.writeObject(toWriteArm);
			
			objectOutput.close();
			outputStream.close();
			
		} catch (IOException e) {
			
			SurvivalGames.logger().logMessage(Language.INVENTORY_SERIALIZATION_ERROR.toString() + this.playerUUID);
			e.printStackTrace();
			
		}
		
	}
	
	/** Used to unserialize and return all inventories of the player UUIDs given. Won't delete the inventory.
	 * @param playerUUIDs A String list of all the names of the player who need their inventories restored. UUIDs are needed, but this method
	 * will autmoatically convert the names into UUIDs
	 * 
	 */
	public static void unserializeInventories(List<String> playerNames) {
		
		if (playerNames == null) return;
		
		List<String> playerUUIDs = new ArrayList<String>();
		
		for (String s : playerNames) {
			
			Player p = Bukkit.getPlayer(s);
			
			if (p != null) {
				
				playerUUIDs.add(p.getUniqueId().toString());
				
			}
			
		}
		
		Thread t = new Thread(new InventoryRetriever(playerUUIDs, "SurvivalGames-activates/inventories/", Bukkit.getPluginManager().getPlugin(SurvivalGames.getPluginName())));
		t.start();
		
	}
	
	/** Used to check if the required directories exist for the storing of player inventories
	 * @return Will return true if the required directories exist
	 */
	private boolean directoryExists() {
		
		File invDirectory = new File("SurvivalGames-activates/inventories");
		
		if (!invDirectory.exists()) {
			return false;
		} else {
			return true;
		}
		
	}
	
	/** A method simply for creating the necessary directories to hold all player's serialized inventories
	 * 
	 */
	private void mkDirs() {
		
		File invDirectory = new File("SurvivalGames-activates/inventories");
		
		invDirectory.mkdirs();
		invDirectory.mkdir();
		
	}
	
	private static class InventoryRetriever implements Runnable {

		List<String> playerUUIDs;
		String dirPath;
		Plugin plugin;
		public InventoryRetriever(List<String> playerUUIDs, String dirPath, Plugin plugin) {
			
			this.playerUUIDs = playerUUIDs;
			this.dirPath = dirPath;
			this.plugin = plugin;
			
		}
		
		@Override
		public void run() {
			
			// storing all of the data in these maps
			Map<String,Object> baseData = new HashMap<String,Object>();
			Map<String,Object> armorData = new HashMap<String,Object>();
			
			for (String uuid : playerUUIDs) {
				
				try {
					
					FileInputStream inputStream = new FileInputStream(dirPath + uuid + "-base.inv");
					ObjectInputStream objectInput = new ObjectInputStream(inputStream);
				
					baseData.put(uuid, objectInput.readObject());
				
					objectInput.close();
					inputStream.close();
					
					inputStream = new FileInputStream(dirPath + uuid + "-armor.inv");
					objectInput = new ObjectInputStream(inputStream);
					
					armorData.put(uuid, objectInput.readObject());
					
					objectInput.close();
					inputStream.close();
					
				} catch (IOException | ClassNotFoundException e) {
					
					SurvivalGames.logger().logMessage("There was an error while deserializing the inventory for UUID: " + uuid);
					
				}
				
			}
			
			// converting it to a final data type...
			final Map<String,Object> baseDataFinal = new HashMap<String,Object>();
			final Map<String,Object> armorDataFinal = new HashMap<String,Object>();
			
			for (String s : baseData.keySet()) { baseDataFinal.put(s, baseData.get(s)); }
			for (String s : armorData.keySet()) { armorDataFinal.put(s, armorData.get(s)); }
			
			// once all the data is stored, we need to make sure to set the player's inventories in a sync thread.. so we'll call the Bukkit scheduler
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				
				public void run() {
					
					// looping through each UUID, getting the player object for that UUID, getting their inventory data for that player, then setting it (complicated!)
					for (String uuid : playerUUIDs) {
						
						Player p = Bukkit.getPlayer(UUID.fromString(uuid));
						
						// checking if the player exists
						if (p != null) {
							
							// checking to make sure they have an object inside base inventory map
							Object baseItems = baseDataFinal.get(uuid);
							Object armorItems = armorDataFinal.get(uuid);
							
							if (baseItems != null && armorItems != null) {
								
								// if they have an inventory stored, this code will run to restore it
								
								@SuppressWarnings("unchecked")
								ArrayList<InventorySerializer> serBaseInventory = (ArrayList<InventorySerializer>) baseItems;
								@SuppressWarnings("unchecked")
								ArrayList<InventorySerializer> serArmorInventory = (ArrayList<InventorySerializer>) armorItems;
								
								List<ItemStack> contents = new ArrayList<ItemStack>();
								
								for (InventorySerializer ser : serBaseInventory) {
									
									contents.add(ser.getItemStack());
									
								}
								
								ItemStack[] toSetBase = new ItemStack[contents.size()];
								toSetBase = contents.toArray(toSetBase);
								
								p.getInventory().setContents(toSetBase);
								
								List<ItemStack> armorContents = new ArrayList<ItemStack>();
								
								for (InventorySerializer ser : serArmorInventory) {
									
									armorContents.add(ser.getItemStack());
									
								}
								
								ItemStack[] toSetArmor = new ItemStack[armorContents.size()];
								toSetArmor = armorContents.toArray(toSetArmor);
								
								p.getInventory().setArmorContents(toSetArmor);
								
							}
							
							
						}
						
					}
					
				}
				
			});
			
		}
		
		
		
	}
}
