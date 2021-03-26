package com.github.maxopoly.KiraBukkitGateway.rabbit.in;

import com.github.maxopoly.KiraBukkitGateway.KiraBukkitGatewayPlugin;
import com.github.maxopoly.KiraBukkitGateway.impersonation.PseudoPlayer;
import com.github.maxopoly.KiraBukkitGateway.rabbit.RabbitInput;
import com.google.gson.JsonObject;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.group.Group;

public class GroupChatMessageHandler extends RabbitInput {

	public GroupChatMessageHandler() {
		super("sendgroupmessage");
	}

	@Override
	public void handle(JsonObject input) {
		UUID sender = UUID.fromString(input.get("sender").getAsString());
		String message = input.get("message").getAsString();
		String groupName = input.get("group").getAsString();
		Group group = GroupManager.getGroup(groupName);
		Logger logger = KiraBukkitGatewayPlugin.getInstance().getLogger();
		if (group == null) {
			logger.severe("Tried to send message to group " + groupName + ", but it wasnt found");
			return;
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(KiraBukkitGatewayPlugin.getInstance(), () -> {
			CivChat2.getInstance().getCivChat2Manager().sendGroupMsg(new PseudoPlayer(sender, -1), group, message);
		});
	}
}
