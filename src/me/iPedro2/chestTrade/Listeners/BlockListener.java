package me.iPedro2.chestTrade.Listeners;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import me.iPedro2.chestTrade.ChestTrade;
import me.iPedro2.chestTrade.Managers.InventoryManager;
import me.iPedro2.chestTrade.Managers.TradeManager;

public class BlockListener implements Listener{

	private ChestTrade plugin;
	private String prefix;
	private InventoryManager invManager;
	private TradeManager trades;

	public BlockListener(ChestTrade plugin, String prefix){
		this.plugin = plugin;
		this.prefix = prefix;
		this.invManager = plugin.invManager;
		this.trades = plugin.trades;
	}

	public void setPrefix(String prefix){
		this.prefix = prefix;
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	private void onChestPlace(BlockPlaceEvent event){
		Block placedChest = event.getBlockPlaced();
		Player player = event.getPlayer();
		if(!placedChest.getType().equals(Material.CHEST)) return;
		Block nearby1;
		Block nearby2;
		int n = -1;
		for(int i = 0; i < 2; i++){
			if(i == 1)
				n = 1;
			nearby1 = placedChest.getLocation().add(n,0,0).getBlock();
			nearby2 = placedChest.getLocation().add(0,0,n).getBlock();
			if((nearby1.getType().equals(Material.CHEST) && invManager.isRegistered(nearby1)) || (nearby2.getType().equals(Material.CHEST) && invManager.isRegistered(nearby2))){
				player.sendMessage(prefix + ChatColor.RED + "You can't place a chest there.");
				event.setCancelled(true);
				return;
			}
		}
		if(!invManager.hasPendingSelection(player)) return;
		n = -1;
		for(int i = 0; i < 2; i++){
			if(i == 1)
				n = 1;
			nearby1 = placedChest.getLocation().add(n,0,0).getBlock();
			nearby2 = placedChest.getLocation().add(0,0,n).getBlock();
			if(nearby1.getType().equals(Material.CHEST) || nearby2.getType().equals(Material.CHEST)){
				player.sendMessage(prefix + ChatColor.RED + "You can only trade using single chests.");
				event.setCancelled(true);
				return;
			}
		}
		if(invManager.hasChest(player)) return;
		invManager.newChest(player, placedChest);
		trades.newTrade(player, plugin.getAssociatedName(player));
		invManager.delPendingSelection(player);
		player.sendMessage(prefix + ChatColor.GOLD + "Chest selected.");
		player.sendMessage(ChatColor.GOLD + "Left click to view your partner's chest.");
		player.sendMessage(ChatColor.GOLD + "Right click to edit your chest's contents.");
		player.sendMessage(ChatColor.YELLOW + "Use \"/ctready\" when you're done.");
		return;
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	private void onChestBreak(BlockBreakEvent event){
		Block brokenChest = event.getBlock();
		Player player = event.getPlayer();
		if(!brokenChest.getType().equals(Material.CHEST)) return;
		if(!invManager.isRegistered(brokenChest)) return;
		for(Entry<String, Block> entry : invManager.chestEntrySet()){
			if(entry.getKey().equals(player.getName()) && entry.getValue().equals(brokenChest)){
				player.sendMessage(prefix + ChatColor.RED + "You can't remove your chest right now.");
				player.sendMessage(ChatColor.RED + "You must cancel the ongoing trade first.");
				event.setCancelled(true);
				return;
			}
		}
		player.sendMessage(prefix + ChatColor.RED + "You're not allowed to remove that chest.");
		event.setCancelled(true);
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	private void onChestExplode(EntityExplodeEvent event){
		List<Block> listBlocks = event.blockList();
		Iterator<Block> it = listBlocks.iterator();
		while(it.hasNext()){
			Block block = it.next();
			if(invManager.isRegistered(block))
				it.remove();
		}
	}
}
