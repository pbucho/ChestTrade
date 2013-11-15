package me.iPedro2.chestTrade.Managers;

import java.util.HashMap;

import org.bukkit.entity.Player;

import me.iPedro2.chestTrade.ChestTrade;

public class RequestManager {

	private ChestTrade plugin;
	private HashMap<String,String> pendingRequests = new HashMap<String,String>();

	public RequestManager(ChestTrade plugin, int requestTimeout){
		this.plugin = plugin;
	}

	public void newRequest(Player receiver, Player sender){
		pendingRequests.put(receiver.getName(), sender.getName());
	}

	public void delRequest(Player receiver){
		pendingRequests.remove(receiver.getName());
	}

	public Player getSender(Player receiver){
		return getPlayer(pendingRequests.get(receiver.getName()));
	}

	public String getSenderName(Player receiver){
		return pendingRequests.get(receiver.getName());
	}

	public boolean hasRequest(Player receiver){
		return pendingRequests.containsKey(receiver.getName());
	}

	public boolean noRequests(){
		return pendingRequests.isEmpty();
	}

	// #########################
	// ##### MISCELLANEOUS #####
	// #########################

	public Player getPlayer(String name){
		return plugin.getServer().getPlayer(name);
	}
}