package com.untamedears.JukeAlert.messages;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.untamedears.JukeAlert.chat.ChatFiller;

public class SendSnitchInfo implements Runnable {
	private List<String> info;
	private Player player;
   
    public SendSnitchInfo(List<String> info, Player player) {
        this.info = info;
        this.player = player;
    }
   
    public void run() {
        player.sendMessage(ChatColor.WHITE + " Snitch Log " + ChatColor.DARK_GRAY + "----------------------------------------");
        player.sendMessage(ChatColor.GRAY + String.format("  %s %s %s", ChatFiller.fillString("Name", (double) 25), ChatFiller.fillString("Reason", (double) 20), ChatFiller.fillString("Details", (double) 30)));
        if (info != null) {
            for (String dataEntry : info) {
                player.sendMessage(dataEntry);
            }
            player.sendMessage("");
        } else {
            player.sendMessage(ChatColor.AQUA + "Page is empty");
        }

    }
}