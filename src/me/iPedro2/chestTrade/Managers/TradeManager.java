package me.iPedro2.chestTrade.Managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

import me.iPedro2.chestTrade.ChestTrade;

public class TradeManager {

	private ChestTrade plugin;
	private HashSet<String> flaggedAsReady = new HashSet<String>();
	private HashSet<String> confirmed = new HashSet<String>();
	private HashMap<String,String> ongoingTrades = new HashMap<String,String>();
	private HashMap<String,Integer> tradesId = new HashMap<String,Integer>();
	private int tradeTimeout;

	public TradeManager(ChestTrade plugin, int tradeTimeout){
		this.plugin = plugin;
		this.tradeTimeout = tradeTimeout;
	}

	public void setTradeTimeout(int timeout){
		this.tradeTimeout = timeout;
	}

	public int getTradeTimeout(){
		return tradeTimeout;
	}

	// ##################################
	// ##### ongoingTrades HANDLERS #####
	// ##################################

	public void newTrade(Player sender, Player receiver){
		ongoingTrades.put(sender.getName(), receiver.getName());
	}

	public void delTrade(Player player){
		ongoingTrades.remove(player.getName());
	}

	public Player getPartner(Player player){
		return getPlayer(ongoingTrades.get(player.getName()));
	}

	public boolean isTrading(Player player){
		return ongoingTrades.containsKey(player.getName());
	}

	public Set<Entry<String,String>> tradesEntrySet(){
		return ongoingTrades.entrySet();
	}

	public boolean noOneTrading(){
		return ongoingTrades.isEmpty();
	}

	// ###################################
	// ##### flaggedAsReady HANDLERS #####
	// ###################################

	public void flagAsReady(Player player){
		flaggedAsReady.add(player.getName());
	}

	public void unflagAsReady(Player player){
		flaggedAsReady.remove(player.getName());
	}

	public boolean isFlaggedAsReady(Player player){
		return flaggedAsReady.contains(player.getName());
	}

	public boolean noOneFlagged(Player player){
		return flaggedAsReady.isEmpty();
	}

	// ##############################
	// ##### confirmed HANDLERS #####
	// ##############################

	public void confirm(Player player){
		confirmed.add(player.getName());
	}

	public void unconfirm(Player player){
		confirmed.remove(player.getName());
	}

	public boolean isConfirmed(Player player){
		return confirmed.contains(player.getName());
	}

	public boolean noOneConfirmed(Player player){
		return confirmed.isEmpty();
	}

	// #############################
	// ##### tradesId HANDLERS #####
	// #############################

	public void newId(Player player, int id){
		tradesId.put(player.getName(), id);
	}

	public void delId(Player player){
		tradesId.remove(player.getName());
	}

	public int getId(Player player){
		return tradesId.get(player.getName());
	}
	
	public boolean hasId(Player player){
		return tradesId.containsKey(player.getName());
	}

	// #########################
	// ##### MISCELLANEOUS #####
	// #########################

	public Player getPlayer(String name){
		return plugin.getServer().getPlayer(name);
	}
}
