package me.iPedro2.chestTrade.Listeners;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import me.iPedro2.chestTrade.ChestTrade;
import me.iPedro2.chestTrade.Managers.InventoryManager;
import me.iPedro2.chestTrade.Managers.TradeManager;

public class PlayerListener implements Listener{

	private ChestTrade plugin;
	private String prefix;
	private InventoryManager invManager;
	private TradeManager trades;

	public PlayerListener(ChestTrade plugin, String prefix){
		this.plugin = plugin;
		this.prefix = prefix;
		this.invManager = plugin.invManager;
		this.trades = plugin.trades;
	}

	public void setPrefix(String prefix){
		this.prefix = prefix;
	}

	@EventHandler
	private void onLeftClick(PlayerInteractEvent event){
		if(!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;
		Player player = event.getPlayer();
		Block chest = event.getClickedBlock();
		if(!event.getClickedBlock().getType().equals(Material.CHEST)) return;
		if(invManager.hasPendingSelection(player)){
			if(invManager.isRegistered(chest)){
				player.sendMessage(prefix + ChatColor.RED + "That chest is already registered.");
				return;
			}
			int n = -1;
			for(int i = 0; i < 2; i++){
				if(i == 1)
					n = 1;
				if(chest.getLocation().add(n,0,0).getBlock().getType().equals(Material.CHEST) || chest.getLocation().add(0,0,n).getBlock().getType().equals(Material.CHEST)){
					player.sendMessage(prefix + ChatColor.RED + "You can only trade using single chests.");
					return;
				}
			}
			invManager.newChest(player, chest);
			trades.newTrade(player, plugin.getAssociatedName(player));
			invManager.delPendingSelection(player);
			player.sendMessage(prefix + ChatColor.GOLD + "Chest selected.");
			player.sendMessage(ChatColor.GOLD + "Left click to view your partner's chest.");
			player.sendMessage(ChatColor.GOLD + "Right click to edit your chest's contents.");
			player.sendMessage(ChatColor.YELLOW + "Use \"/ctready\" when you're done.");
			return;
		}
		for(Entry<String, Block> entry : invManager.chestEntrySet()){
			if(entry.getKey().equals(player.getName()) && entry.getValue().equals(chest)){
				if(!invManager.hasChest(trades.getPartner(player))){
					player.sendMessage(prefix + ChatColor.RED + plugin.getAssociatedName(player).getName() + " hasn't selected a chest yet.");
					return;
				}
				Chest partnersChest = (Chest) invManager.getChestBlock(trades.getPartner(player)).getState();
				Inventory partnersChestInv = plugin.getServer().createInventory(null, InventoryType.CHEST);
				partnersChestInv.setContents(partnersChest.getInventory().getContents());
				player.openInventory(partnersChestInv);
				invManager.setViewingPartnersChest(player,true);
				return;
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	private void onRightClick(PlayerInteractEvent event){
		if(!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if(!block.getType().equals(Material.CHEST)) return;
		if(!invManager.isRegistered(block)) return;
		if(invManager.getChestBlock(player).equals(block)){
			if(trades.isFlaggedAsReady(player)){
				if(trades.isConfirmed(player)){
					player.sendMessage(prefix + ChatColor.RED + "You can't edit your chest at this point.");
					event.setCancelled(true);
					return;
				}
				Player partner = plugin.getAssociatedName(player); 
				player.sendMessage(prefix + ChatColor.RED + "You were un-flagged as ready.");
				partner.sendMessage(prefix + ChatColor.RED + partner.getName() + " is not ready.");
				trades.unflagAsReady(player);
				if(trades.isConfirmed(partner)){
					trades.unconfirm(partner);
					partner.sendMessage(prefix + ChatColor.RED + player.getName() + " has changed their chest's contents.");
					partner.sendMessage(ChatColor.RED + "Adjust your offer to your partner's offer, if you want to.");
				}
			}
			return;
		}
		player.sendMessage(prefix + ChatColor.RED + "You're not allowed to open that chest.");
		event.setCancelled(true);
		return;
	}

	@EventHandler
	private void onPlayerCloseInv(InventoryCloseEvent event){
		if(!trades.isTrading((Player) event.getPlayer())) return;
		if(!invManager.isViewingPartnersChest((Player) event.getPlayer())) return;
		invManager.setViewingPartnersChest((Player) event.getPlayer(),false);
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	private void onPlayerMovePartnersInv(InventoryClickEvent event){
		if(!trades.isTrading((Player) event.getWhoClicked())) return;
		if(!invManager.isViewingPartnersChest((Player) event.getWhoClicked())) return;
		event.setCancelled(true);
	}

	@EventHandler
	private void onPlayerLogout(PlayerQuitEvent event){
		Player player = event.getPlayer();
		if(!plugin.hasAssociatedName(player)) return;
		Player partner = plugin.getAssociatedName(player);
		partner.sendMessage(prefix + ChatColor.RED + player.getName() + " logged out. Trade terminated.");
		plugin.cleanupPlayerAndPartnerData(player);
	}

	@EventHandler
	private void onPlayerDeath(PlayerDeathEvent event){
		Player player = event.getEntity();
		if(!plugin.hasAssociatedName(player)) return;
		Player partner = plugin.getAssociatedName(player);
		partner.sendMessage(prefix + ChatColor.RED + player.getName() + " died. Trade terminated.");
		plugin.cleanupPlayerAndPartnerData(player);
	}

	public Player getPlayer(String name){
		return plugin.getServer().getPlayer(name);
	}
}
