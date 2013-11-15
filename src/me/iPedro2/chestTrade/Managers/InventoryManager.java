package me.iPedro2.chestTrade.Managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.iPedro2.chestTrade.ChestTrade;

public class InventoryManager {

	private ChestTrade plugin;
	private HashMap<String,Block> registeredChests = new HashMap<String,Block>();
	private HashSet<String> pendingSelections = new HashSet<String>();
	private HashSet<String> viewingPartnersChest = new HashSet<String>();

	public InventoryManager(ChestTrade plugin, int selectionTimeout){
		this.plugin = plugin;
	}

	// #########################################
	// ##### viewingPartnersChest HANDLERS #####
	// #########################################

	public void setViewingPartnersChest(Player player, boolean b){
		if(b)
			viewingPartnersChest.add(player.getName());
		else{
			if(viewingPartnersChest.contains(player.getName()))
				viewingPartnersChest.remove(player.getName());
		}
	}

	public boolean isViewingPartnersChest(Player player){
		return viewingPartnersChest.contains(player.getName());
	}

	public boolean noOneViewing(){
		return viewingPartnersChest.isEmpty();
	}

	// ######################################
	// ##### pendingSelections HANDLERS #####
	// ######################################

	public void newPendingSelection(Player player){
		pendingSelections.add(player.getName());
	}

	public void delPendingSelection(Player player){
		pendingSelections.remove(player.getName());
	}

	public boolean hasPendingSelection(Player player){
		return pendingSelections.contains(player.getName());
	}

	public boolean noPendingSelections(){
		return pendingSelections.isEmpty();
	}

	// #####################################
	// ##### registeredChests HANDLERS #####
	// #####################################

	public void newChest(Player player, Block chest){
		registeredChests.put(player.getName(), chest);
	}

	public void delChest(Player player){
		registeredChests.remove(player.getName());
	}

	public Block getChestBlock(Player player){
		return registeredChests.get(player.getName());
	}

	public Inventory getChestInventory(Player player){
		Chest theChest = (Chest) registeredChests.get(player.getName()).getState();
		return theChest.getInventory();
	}

	public boolean isRegistered(Block chest){
		return registeredChests.containsValue(chest);
	}

	public boolean hasChest(Player player){
		return registeredChests.containsKey(player.getName());
	}

	public Set<Entry<String,Block>> chestEntrySet(){
		return registeredChests.entrySet();
	}

	// #########################
	// ##### MISCELLANEOUS #####
	// #########################

	public Player getPlayer(String name){
		return plugin.getServer().getPlayer(name);
	}
}
