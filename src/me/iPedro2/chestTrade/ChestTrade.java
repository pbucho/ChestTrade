package me.iPedro2.chestTrade;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

import me.iPedro2.chestTrade.Listeners.*;
import me.iPedro2.chestTrade.Managers.*;

public class ChestTrade extends JavaPlugin{

	private RequestManager requests = new RequestManager(this, 0);
	public InventoryManager invManager = new InventoryManager(this, 0);
	public TradeManager trades = new TradeManager(this, 0);
	private HashMap<String,String> associatedNames = new HashMap<String,String>();
	public String prefix;
	public PlayerListener playerListener = new PlayerListener(this,null);
	public BlockListener blockListener = new BlockListener(this,null);
	private boolean metrics = true;
	Logger log = Bukkit.getLogger();
	File configFile = new File(getDataFolder() + "config.yml");

	@Override
	public void onEnable(){
		Bukkit.getServer().getPluginManager().registerEvents(playerListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(blockListener, this);
		if (!(configFile.exists())) {
			this.saveDefaultConfig();
		}
		try{
			prefix = String.valueOf(this.getConfig().getString("prefix")) + ChatColor.RESET;
			int time;
			time = Math.abs(Integer.valueOf(this.getConfig().getInt("tradeTimeout")));
			if(time == 0)
				time = 300;
			trades.setTradeTimeout(time);
			metrics = Boolean.valueOf(this.getConfig().getBoolean("metrics"));
		}catch(final Exception e){
			log.severe("Error loading config file.");
			e.printStackTrace();
		}
		if(metrics){
			try {
				MetricsLite metrics = new MetricsLite(this);
				metrics.start();
				log.info("Metrics enabled.");
			} catch (IOException e) {
				// nothing
			}
		}
		playerListener.setPrefix(prefix);
		blockListener.setPrefix(prefix);
	}

	@Override
	public void onDisable(){
		if(!noAssociatedNames()){
			for(Entry<String, String> entry : associatedNames.entrySet()){
				Player player = getPlayer(entry.getKey());
				player.sendMessage(prefix + ChatColor.RED + "Trade terminated due to a server reload/shutdown.");
			}
		}
	}

	public boolean onCommand(CommandSender issuer, Command cmd, String commandLabel, String[] args){
		commandLabel.toLowerCase();
		if(commandLabel.equalsIgnoreCase("cttrade")){
			if(args.length < 1)
				sendRequest(issuer, null);
			else
				sendRequest(issuer, args[0]);
		}else if(commandLabel.equalsIgnoreCase("ctrefuse") || commandLabel.equalsIgnoreCase("ctdeny"))
			refuseRequest(issuer);
		else if(commandLabel.equalsIgnoreCase("ctaccept"))
			acceptRequest(issuer);
		else if(commandLabel.equalsIgnoreCase("ctready"))
			markAsReady(issuer);
		else if(commandLabel.equalsIgnoreCase("ctconfirm"))
			confirmTrade(issuer);
		else if(commandLabel.equalsIgnoreCase("ctcancel"))
			cancelTrade(issuer);
		else if(commandLabel.equalsIgnoreCase("ctver"))
			displayVersion(issuer);
		else if(commandLabel.equalsIgnoreCase("ctreload"))
			reloadCfg(issuer);
		else if(commandLabel.equalsIgnoreCase("ct"))
			displayHelpPage(issuer);
		return true;
	}

	private void displayHelpPage(CommandSender issuer) {
		if(!issuer.hasPermission("ChestTrade.help")){
			issuer.sendMessage(prefix + ChatColor.RED + "You do not have permission to view the help menu.");
			return;
		}
		issuer.sendMessage(ChatColor.GREEN + "===== ChestTrade help page =====");
		issuer.sendMessage("/ct - " + ChatColor.GREEN + "Displays this help page.");
		issuer.sendMessage("/cttrade <player> - " + ChatColor.GREEN + "Sends a trade request to <player>.");
		issuer.sendMessage("/ctrefuse - " + ChatColor.GREEN + "Refuses a trade request sent by another player.");
		issuer.sendMessage("/ctdeny - " + ChatColor.GREEN + "The same as /ctrefuse.");
		issuer.sendMessage("/ctaccept - " + ChatColor.GREEN + "Accepts a trade request sent by another player.");
		issuer.sendMessage("/ctcancel - " + ChatColor.GREEN + "Cancels an ongoing trade.");
		issuer.sendMessage("/ctready - " + ChatColor.GREEN + "Marks a player as ready.");
		issuer.sendMessage("/ctconfirm - " + ChatColor.GREEN + "Confirms a trade.");
		issuer.sendMessage("/ctver - " + ChatColor.GREEN + "Displays the plugin version.");
		issuer.sendMessage("/ctreload - " + ChatColor.GREEN + "Reloads the config file.");
	}

	private void reloadCfg(CommandSender issuer) {
		if(!issuer.hasPermission("ChestTrade.reload")){
			issuer.sendMessage(prefix + ChatColor.RED + "You do not have permission to reload the config file.");
			return;
		}
		this.reloadConfig();
		if (!(configFile.exists())) {
			this.saveDefaultConfig();
		}
		try{
			prefix = String.valueOf(this.getConfig().getString("prefix")) + ChatColor.RESET;
			int time;
			time = Math.abs(Integer.valueOf(this.getConfig().getInt("tradeTimeout")));
			if(time == 0)
				time = 300;
			trades.setTradeTimeout(time);
			metrics = Boolean.valueOf(this.getConfig().getBoolean("metrics"));
		}catch(final Exception e){
			log.severe("Error loading config file.");
			e.printStackTrace();
		}
		issuer.sendMessage(prefix + ChatColor.GOLD + "Config file reloaded.");
	}

	private void confirmTrade(CommandSender issuer) {
		if(!(issuer instanceof Player)){
			issuer.sendMessage(prefix + ChatColor.RED + "This command must be run by an in-game player.");
			return;
		}
		Player player = (Player) issuer;
		if(!trades.isTrading(player)){
			player.sendMessage(prefix + ChatColor.RED + "You're not in any trades.");
			return;
		}
		if(!trades.isFlaggedAsReady(player)){
			player.sendMessage(prefix + ChatColor.RED + "You must use \"/ctready\" before using this command.");
			return;
		}
		Player partner = getAssociatedName(player);
		if(!trades.isFlaggedAsReady(partner)){
			player.sendMessage(prefix + ChatColor.RED + "Your partner isn't ready yet.");
			return;
		}
		trades.confirm(player);
		if(!trades.isConfirmed(partner)){
			player.sendMessage(prefix + ChatColor.GOLD + "Trade confirmed. You can no longer edit your chest.");
			partner.sendMessage(prefix + ChatColor.GREEN + player.getName() + " accepted your offer.");
			partner.sendMessage(ChatColor.GOLD + "If you accept your partner's offer, type \"/ctconfirm\".");
			return;
		}
		swapInventories(player);
		player.sendMessage(prefix + ChatColor.GREEN + "Trade completed!");
		partner.sendMessage(prefix + ChatColor.GREEN + "Trade completed!");
		cleanupPlayerAndPartnerData(player);
	}

	private void swapInventories(Player player) {
		Player partner = getAssociatedName(player);
		Inventory tmpInv = this.getServer().createInventory(null, InventoryType.CHEST);
		Inventory playerInv = invManager.getChestInventory(player);
		Inventory partnerInv = invManager.getChestInventory(partner);
		tmpInv.setContents(partnerInv.getContents());
		partnerInv.setContents(playerInv.getContents());
		playerInv.setContents(tmpInv.getContents());
		tmpInv.clear();
	}

	private void cancelTrade(CommandSender issuer) {
		if(!(issuer instanceof Player)){
			issuer.sendMessage(prefix + ChatColor.RED + "This command must be run by an in-game player.");
			return;
		}
		Player player = (Player) issuer;
		if(!hasAssociatedName(player)){
			player.sendMessage(prefix + ChatColor.RED + "You're not in any trades.");
			return;
		}
		Player partner = getAssociatedName(player);
		player.sendMessage(prefix + ChatColor.RED + "Trade with " + partner.getName() + " cancelled.");
		partner.sendMessage(prefix + ChatColor.RED + player.getName() + " cancelled the trade.");
		cleanupPlayerAndPartnerData(player);
	}

	private void markAsReady(CommandSender issuer) {
		if(!(issuer instanceof Player)){
			issuer.sendMessage(prefix + ChatColor.RED + "This command must be run by an in-game player.");
			return;
		}
		Player player = (Player) issuer;
		if(!hasAssociatedName(player)){
			player.sendMessage(prefix + ChatColor.RED + "You're not in any trades.");
			return;
		}
		if(!trades.isTrading(player)){
			player.sendMessage(prefix + ChatColor.RED + "You haven't selected a chest yet.");
			return;
		}
		if(trades.isFlaggedAsReady(player)){
			player.sendMessage(prefix + ChatColor.RED + "You're already flagged as ready.");
			return;
		}
		Player partner = getAssociatedName(player);
		player.sendMessage(prefix + ChatColor.GOLD + "You were flagged as ready.");
		player.sendMessage(ChatColor.GOLD + "Opening your chest will un-flag you.");
		partner.sendMessage(prefix + ChatColor.GREEN + player.getName() + " is ready.");
		if(!trades.isFlaggedAsReady(partner))
			partner.sendMessage(ChatColor.GOLD + "If you're also ready, type \"/ctready\".");
		trades.flagAsReady(player);
		if(trades.isFlaggedAsReady(partner)){
			player.sendMessage(prefix + ChatColor.GOLD + "You are both flagged as ready.");
			player.sendMessage(ChatColor.GOLD + "Check your partner's inventory and use \"/ctconfirm\" to proceed.");
			partner.sendMessage(prefix + ChatColor.GOLD + "You are both flagged as ready.");
			partner.sendMessage(ChatColor.GOLD + "Check your partner's inventory and use \"/ctconfirm\" to proceed.");
		}
	}

	private void acceptRequest(CommandSender issuer) {
		if(!(issuer instanceof Player)){
			issuer.sendMessage(prefix + ChatColor.RED + "This command must be run by an in-game player.");
			return;
		}
		Player receiver = (Player) issuer;
		if(!requests.hasRequest(receiver)){
			receiver.sendMessage(prefix + ChatColor.RED + "You don't have any pending requests.");
			return;
		}
		if(!receiver.getGameMode().equals(GameMode.SURVIVAL)){
			receiver.sendMessage(prefix + ChatColor.RED + "You can only trade whilst in survival mode.");
			return;
		}
		Player sender = requests.getSender(receiver);
		requests.delRequest(receiver);
		receiver.sendMessage(prefix + ChatColor.GOLD + "Trade request accepted.");
		sender.sendMessage(prefix + ChatColor.GOLD + receiver.getName() + " accepted your trade request.");
		handleSelection(sender,receiver);
	}

	private void handleSelection(Player sender, Player receiver) {
		invManager.newPendingSelection(sender);
		invManager.newPendingSelection(receiver);
		invManager.newPendingSelection(sender);
		invManager.newPendingSelection(receiver);
		sender.sendMessage(prefix + ChatColor.GOLD + "Left click or place down a chest to initiate trade.");
		receiver.sendMessage(prefix + ChatColor.GOLD + "Left click or place down a chest to initiate trade.");
	}

	private void refuseRequest(CommandSender issuer) {
		if(!(issuer instanceof Player)){
			issuer.sendMessage(prefix + ChatColor.RED + "This command must be run by an in-game player.");
			return;
		}
		Player receiver = (Player) issuer;
		if(!requests.hasRequest(receiver)){
			receiver.sendMessage(prefix + ChatColor.RED + "You don't have any pending requests.");
			return;
		}
		Player sender = requests.getSender(receiver);
		receiver.sendMessage(prefix + ChatColor.GOLD + "Trade request refused.");
		sender.sendMessage(prefix + ChatColor.RED + receiver.getName() + " refused your trade request.");
		cleanupPlayerAndPartnerData(receiver);
	}

	private void sendRequest(CommandSender issuer, String args){
		if(!issuer.hasPermission("ChestTrade.trade")){
			issuer.sendMessage(prefix + ChatColor.RED + "You do not have permission to trade with other players.");
			return;
		}
		if(!(issuer instanceof Player)){
			issuer.sendMessage(prefix + ChatColor.RED + "This command must be run by an in-game player.");
			return;
		}
		Player sender = (Player) issuer;
		if(!sender.getGameMode().equals(GameMode.SURVIVAL)){
			sender.sendMessage(prefix + ChatColor.RED + "You can only trade whilst in survival mode.");
			return;
		}
		if(hasAssociatedName(sender)){
			sender.sendMessage(prefix + ChatColor.RED + "You can't initiate a new trade whilst the current one isn't finished.");
			return;
		}
		if(args == null){
			sender.sendMessage(prefix + ChatColor.RED + "Enter the name of the player you want to trade with.");
			sender.sendMessage(ChatColor.RED + "\"/cttrade <player name>\"");
			return;
		}
		if(sender.getName().equalsIgnoreCase(args)){
			sender.sendMessage(prefix + ChatColor.RED + "How do you trade items with yourself?");
			return;
		}
		Player receiver = getPlayer(args);
		if(receiver == null){
			sender.sendMessage(prefix + ChatColor.RED + "Player not found.");
			return;
		}
		if(!receiver.hasPermission("ChestTrade.trade")){
			sender.sendMessage(prefix + ChatColor.RED + "The selected player does not have permission to trade.");
			return;
		}
		if(hasAssociatedName(receiver)){
			sender.sendMessage(prefix + ChatColor.RED + "The selected player is in a trade process.");
			sender.sendMessage(ChatColor.RED + "Wait until they're finished.");
			return;
		}
		addAssociatedNames(sender, receiver);
		addAssociatedNames(receiver, sender);
		requests.newRequest(receiver, sender);
		setExpireTrade(sender);
		receiver.sendMessage(prefix + ChatColor.GOLD + sender.getName() + " wants to trade with you.");
		receiver.sendMessage(ChatColor.GOLD + "Use \"/ctaccept\" to accept or \"/ctrefuse\" to refuse.");
		sender.sendMessage(prefix + ChatColor.GOLD + "Request sent.");
		return;
	}

	public void cleanupPlayerData(Player player){
		if(requests.hasRequest(player)){
			requests.delRequest(player);
		}
		if(invManager.hasChest(player))
			invManager.delChest(player);
		if(invManager.hasPendingSelection(player)){
			invManager.delPendingSelection(player);
		}
		if(invManager.isViewingPartnersChest(player)){
			invManager.setViewingPartnersChest(player, false);
			player.closeInventory();
		}
		if(trades.hasId(player)){
			cancelTradeExpiration(player);
		}
		if(trades.isTrading(player)){
			trades.delTrade(player);
		}
		if(trades.isFlaggedAsReady(player))
			trades.unflagAsReady(player);
		if(trades.isConfirmed(player)){
			trades.unconfirm(player);
		}
		delAssociatedNames(player);
	}

	public void cleanupPlayerAndPartnerData(Player player){
		Player partner = getAssociatedName(player);
		cleanupPlayerData(player);
		cleanupPlayerData(partner);
	}

	public void setExpireTrade(final Player player){
		int requestId = this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
			public void run(){
				Player partner = getAssociatedName(player);
				player.sendMessage(prefix + ChatColor.RED + "Trade with " + partner.getName() + " cancelled.");
				player.sendMessage(ChatColor.RED + "The trade timed out.");
				partner.sendMessage(prefix + ChatColor.RED + "Trade with " + player.getName() + " cancelled.");
				partner.sendMessage(ChatColor.RED + "The trade timed out.");
				cleanupPlayerAndPartnerData(player);
			}
		}, trades.getTradeTimeout()*20);
		trades.newId(player, requestId);
	}

	public void cancelTradeExpiration(final Player player){
		this.getServer().getScheduler().cancelTask(trades.getId(player));
		trades.delId(player);
	}

	private void displayVersion(CommandSender sender){
		if(sender.hasPermission("ChestTrade.ver")){
			PluginDescriptionFile pdf = this.getDescription();
			sender.sendMessage(ChatColor.AQUA + "========== ChestTrade ==========");
			sender.sendMessage(ChatColor.GOLD + "Version: " + pdf.getVersion());
			sender.sendMessage(ChatColor.GOLD + "Author:  " + pdf.getAuthors());
			sender.sendMessage(ChatColor.AQUA + "================================");
		}else{
			sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to view this information.");
		}
	}

	// ####################################
	// ##### associatedNames HANDLERS #####
	// ####################################

	public void addAssociatedNames(Player first, Player second){
		associatedNames.put(first.getName(), second.getName());
	}

	public void delAssociatedNames(Player player){
		associatedNames.remove(player.getName());
	}

	public Player getAssociatedName(Player player){
		if(player == null)
			return null;
		return getPlayer(associatedNames.get(player.getName()));
	}

	public boolean hasAssociatedName(Player player){
		return associatedNames.containsKey(player.getName());
	}

	public boolean noAssociatedNames(){
		return associatedNames.isEmpty();
	}

	public Player getPlayer(String name){
		return this.getServer().getPlayer(name);
	}
}
