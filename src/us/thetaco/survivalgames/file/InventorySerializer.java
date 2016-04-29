package us.thetaco.survivalgames.file;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/** A class for serializing item stacks. It stores every type of data I could think of!
 * @author activates
 *
 */
public class InventorySerializer implements Serializable {
	
	private static final long serialVersionUID = -4654290532678921162L;

	private final String type;
	private final int amount;
	private final short damage;
	private final byte data;

	private boolean isWrittenBook = false;
	private String bookAuthor;
	private String bookTitle;
	private ArrayList<String> bookPages;

	private boolean isBook = false;

	private boolean hasDisplayName = false;
	String displayName;
	
	private boolean hasLore = false;
	private ArrayList<String> lore;
	
	boolean isEnchantedBook = false;
	
	private Map<String, Integer> itemEnchants;

	@SuppressWarnings("deprecation")
	public InventorySerializer(ItemStack i) {

		this.type = i.getType().toString();
		this.amount = i.getAmount();
		this.damage = i.getDurability();
		this.data = i.getData().getData();

		Map<Enchantment,Integer> enchantmentInfo = i.getEnchantments();
		
		if (enchantmentInfo != null) {
			
			itemEnchants = new HashMap<String, Integer>();
			
			for (Enchantment ench : enchantmentInfo.keySet()) {
				
				if (ench != null) {
					if (enchantmentInfo.get(ench) == null) {
						itemEnchants.put(ench.getName(), 1);
					} else {
						itemEnchants.put(ench.getName(), enchantmentInfo.get(ench));
					}
				}
				
			}
		}
		
		if (i.getType() == Material.WRITTEN_BOOK) {

			this.isWrittenBook = true;
			BookMeta bm = (BookMeta) i.getItemMeta();

			if (bm.hasAuthor()) {
				this.bookAuthor = bm.getAuthor();
			} else {
				this.bookAuthor = null;
			}
			
			if (bm.hasTitle()) {
				this.bookTitle = bm.getTitle();
			} else {
				this.bookTitle = null;
			}
			
			ArrayList<String> convertedArrayList = new ArrayList<String>();
			convertedArrayList.addAll(bm.getPages());

			this.bookPages = convertedArrayList;

		} else if (i.getType() == Material.BOOK_AND_QUILL) {
			
			this.isBook = true;
			BookMeta bm = (BookMeta) i.getItemMeta();
			
			if (bm.hasPages()) {
				ArrayList<String> convertedArrayList = new ArrayList<String>();
				convertedArrayList.addAll(bm.getPages());

				this.bookPages = convertedArrayList;
			} else {
				bookPages = new ArrayList<String>();
			}
			
		} else if (i.getType() == Material.ENCHANTED_BOOK) {
			
			this.isEnchantedBook = true;
			
			EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) i.getItemMeta();
			
			enchantmentInfo = enchantMeta.getStoredEnchants();
			
			itemEnchants = new HashMap<String, Integer>();
			
			if (enchantmentInfo != null) {
			
			for (Enchantment ench : enchantmentInfo.keySet()) {
				
					if (ench != null) {
						if (enchantmentInfo.get(ench) == null) {
							itemEnchants.put(ench.getName(), 1);
						} else {
							itemEnchants.put(ench.getName(), enchantmentInfo.get(ench));
						}
					}
					
				}
			
			}
			
		}

		if (i.hasItemMeta()) {
			
			ItemMeta im = i.getItemMeta();
			
			if (im.hasDisplayName()) {
				
				this.hasDisplayName = true;
				
				this.displayName = im.getDisplayName();
				
			}
			
			if (im.hasLore()) {
				
				this.hasLore = true;
				
				ArrayList<String> convertedArrayList = new ArrayList<String>();
				convertedArrayList.addAll(im.getLore());
				
				this.lore = convertedArrayList;
				
			}
			
			
		}
		
	}
	
	public boolean isEnchanted() {

		if (itemEnchants == null || itemEnchants.isEmpty()) {
			return false;
		} else {
			return true;
		}

	}

	public ItemStack getItemStack() {

		@SuppressWarnings("deprecation")
		ItemStack toReturn = new ItemStack(Material.getMaterial(type), this.amount, this.damage, this.data);

		if (this.isEnchanted()) {

			if (this.isEnchantedBook) {
				
				EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) toReturn.getItemMeta();
				
				for (String enchant : this.itemEnchants.keySet()) {
					enchantMeta.addStoredEnchant(Enchantment.getByName(enchant), this.itemEnchants.get(enchant), true);
				}
				
				toReturn.setItemMeta(enchantMeta);
				
			} else {
				
				for (String enchant : this.itemEnchants.keySet()) {
				
					toReturn.addUnsafeEnchantment(Enchantment.getByName(enchant), this.itemEnchants.get(enchant));
					
				}
				
			}
			
		}

		if (this.isWrittenBook) {

			BookMeta bm = (BookMeta) toReturn.getItemMeta();

			bm.setAuthor(this.bookAuthor);
			bm.setPages(this.bookPages);
			bm.setTitle(this.bookTitle);

			toReturn.setItemMeta(bm);

		}

		if (this.isBook) {
			
			BookMeta bm = (BookMeta) toReturn.getItemMeta();
			
			if (this.bookPages != null) {
				
				bm.setPages(this.bookPages);
				
			}
			
			if (bm != null) toReturn.setItemMeta(bm);
			
		}
		
		if (this.hasDisplayName) {
			
			if (this.displayName != null) {
				
				ItemMeta im = toReturn.getItemMeta();
			
				im.setDisplayName(this.displayName);
				
				toReturn.setItemMeta(im);
				
			}
			
		}
		
		if (this.hasLore) {
			
			if (this.lore != null) {
				
				ItemMeta im = toReturn.getItemMeta();
				
				im.setLore(this.lore);
				
				toReturn.setItemMeta(im);
				
			}
			
		}
		
		return toReturn;

	}

}
