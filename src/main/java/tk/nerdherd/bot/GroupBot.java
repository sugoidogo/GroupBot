package tk.nerdherd.bot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Base64;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;

public class GroupBot extends ListenerAdapter {
	public static final String GROUP_CHANNEL_NAME = "groups";
	private static final String REACTION_ADD_VOICE_CHANNEL = "🎙";
	private static final String REACTION_JOIN_GROUP = "👤";
	private static final String REACTION_ADD_TEXT_CHANNEL = "💬";
	private static final String NEW_GUILD_OWNER_MESSAGE = "This bot requires a channel called 'groups' to perform all of it's functions.";
	private static final String NEW_GUILD_NO_CHANNEL = "Please create a new text channel anywhere in your server by that name and I can get started. Note that there needs to be only one channel by this name or bad things will happen!";
	private static final String NEW_GUILD_YES_CHANNEL = "Is this channel made for this bot?";
	private static final String REACTION_YES = "✅";
	private static final String REACTION_NO = "❎";
	private static final String DEFAULT_CHANNEL_NAME = "general";
	public static JDA jda;
	private static HashMap<Long, String> groups = new HashMap<Long, String>();
	private static TextChannel DATA_STORAGE_CHANNEL;
	private static boolean writing = false;

	private boolean deleting = false;

	public static void main(String[] args) {

		System.setProperty("org.slf4j.Loggerger.log.WebSocketClient", "trace");
		/*
		 * Attempt to log in to bot account and register our event listener
		 */
		try {
			jda = new JDABuilder(AccountType.BOT).setToken(args[0]).buildBlocking();
			jda.addEventListener(new GroupBot());
		} catch (LoginException e) {
			// TODO
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO
			e.printStackTrace();
		} catch (RateLimitedException e) {
			// TODO
			e.printStackTrace();
		}

		System.out.println("JDAInfo.Version=" + JDAInfo.VERSION);

		try {
			DATA_STORAGE_CHANNEL = jda.getGuildById(args[1]).getTextChannelsByName(jda.getSelfUser().getName(), true)
					.get(0);
		} catch (Exception e) {
			DATA_STORAGE_CHANNEL = (TextChannel) jda.getGuildById(args[1]).getController()
					.createTextChannel(jda.getSelfUser().getName()).complete();
		}
		try {
			readData(args[1]);
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Rebuilding data set");
			createData(args[1]);
			writeData(groups);
		}
	}

	private static void createData(String ignore) {
		for (Guild guild : jda.getGuilds()) {
			if (!guild.getId().equals(ignore)) {
				TextChannel channel = guild.getTextChannelsByName(GROUP_CHANNEL_NAME, true).get(0);
				for (Message message : channel.getIterableHistory().complete()) {
					newGroup(message);
				}
			}
		}
	}

	public static void writeData(Serializable data) {
		while (writing) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		writing = true;
		for(Message message:DATA_STORAGE_CHANNEL.getIterableHistory().complete()) {
			message.delete().queue();
		}
		DATA_STORAGE_CHANNEL.sendMessage(serialize(data)).complete();
		writing = false;
	}

	public static void readData(String ignore) {
		createData(ignore);
		writeData(groups);
	}

	@Override
	public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
		if (event.getChannel().getName().equals(GROUP_CHANNEL_NAME) && groups.containsKey(event.getMessageIdLong())) {
			deleteGroup(event.getMessageIdLong(), event.getGuild());
		}
	}

	@Override
	public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
		if (event.getRoles().get(0).getName().equals(jda.getSelfUser().getName())) {
			onJoinNewServer(event);
		}
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getChannel().getName().equals(GROUP_CHANNEL_NAME)) {
			newGroup(event.getMessage());
		}
	}

	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		// Don't react outside of the groups channel
		if (!event.getChannel().getName().equals(GROUP_CHANNEL_NAME))
			return;

		// Don't react to reactions from the bot itself
		if (event.getUser().equals(jda.getSelfUser()))
			return;

		// Determine what to do based on the reaction
		String reaction = event.getReactionEmote().getName();
		if (reaction.equals(REACTION_ADD_VOICE_CHANNEL))
			addVoiceChannel(event);
		else if (reaction.equals(REACTION_JOIN_GROUP))
			joinGroup(event);
		else if (reaction.equals(REACTION_ADD_TEXT_CHANNEL))
			addTextChannel(event);
		else if (event.getMember().isOwner()) {
			if (reaction.equals(REACTION_YES))
				onJoinNewServer(event, true);
			else if (reaction.equals(REACTION_NO))
				onJoinNewServer(event, false);
		} else {
			event.getReaction().removeReaction().queue();
		}
	}

	private void deleteGroup(long messageIdLong, Guild guild) {
		while (deleting) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		deleting = true;
		Category category = guild.getCategoriesByName(groups.get(messageIdLong), true).get(0);
		for (Channel channel : category.getChannels()) {
			channel.delete().complete();
		}
		category.delete().complete();
		guild.getRolesByName(groups.get(messageIdLong), true).get(0).delete().queue();
		groups.remove(messageIdLong);
		deleting = false;
		writeData(groups);
	}

	/**
	 * @param event
	 * @param b
	 */
	private void onJoinNewServer(GuildMessageReactionAddEvent event, boolean b) {
		if (b) {
			for (Message message : new MessageHistory(event.getChannel()).retrievePast(100).complete()) {
				newGroup(message);
			}
		} else {
			event.getChannel().sendMessage(NEW_GUILD_NO_CHANNEL);
		}

	}

	/**
	 * @param event
	 */
	private void addTextChannel(GuildMessageReactionAddEvent event) {
		event.getGuild()
				.getCategoriesByName(
						getFirstWord(event.getChannel().getMessageById(event.getMessageId()).complete().getContent()),
						true)
				.get(0).createTextChannel(DEFAULT_CHANNEL_NAME).queue();
		event.getReaction().removeReaction(event.getUser()).queue();
	}

	/**
	 * @param event
	 * 
	 */
	private void addVoiceChannel(GuildMessageReactionAddEvent event) {
		event.getGuild()
				.getCategoriesByName(
						getFirstWord(event.getChannel().getMessageById(event.getMessageId()).complete().getContent()),
						true)
				.get(0).createVoiceChannel(DEFAULT_CHANNEL_NAME).queue();
		event.getReaction().removeReaction(event.getUser()).queue();
	}

	/**
	 * @param event
	 */
	private void joinGroup(GuildMessageReactionAddEvent event) {
		String group = getFirstWord(event.getChannel().getMessageById(event.getMessageId()).complete().getContent());
		Guild guild = event.getGuild();
		guild.getController().addRolesToMember(event.getMember(), guild.getRolesByName(group, true).get(0)).queue();
	}

	/**
	 * @param message
	 */
	private static void newGroup(Message message) {
		if (message.getContent().equals(NEW_GUILD_NO_CHANNEL)) {
			message.delete().queue();
			return;
		}
		if (message.getContent().equals(NEW_GUILD_YES_CHANNEL)) {
			message.delete().queue();
			return;
		}
		if (message.getContent().equals(NEW_GUILD_OWNER_MESSAGE)) {
			message.delete().queue();
			return;
		}

		String groupName = getFirstWord(message.getContent());
		if (groups.containsValue(groupName)) {
			message.delete().queue();
			return;
		}
		Guild guild = message.getGuild();
		GuildController guildController = guild.getController();
		Category category;
		try {
			category = newCategory(guild, guildController, groupName);
		}catch (Exception e) {
			invalidName(message);
			return;
		}
		Role role = newRole(guild, guildController, groupName);
		role.getManager().setName(groupName).queue();
		try {
			category.createPermissionOverride(guild.getPublicRole()).setDeny(Permission.MESSAGE_READ).queue();
		} catch (IllegalStateException e) {

		}
		try {
			category.createPermissionOverride(role).setAllow(Permission.MESSAGE_READ).queue();
		} catch (IllegalStateException e) {

		}
		try {
			category.createPermissionOverride(message.getMember()).setAllow(Permission.ALL_PERMISSIONS).queue();
		} catch (IllegalStateException e) {

		}
		for (Role mentionedRole : message.getMentionedRoles()) {
			category.createPermissionOverride(mentionedRole).setAllow(Permission.MESSAGE_READ).queue();
		}
		for (User mentionedUser : message.getMentionedUsers()) {
			guildController.addRolesToMember(guild.getMember(mentionedUser), role).queue();
		}
		if (category.getChannels().isEmpty()) {
			category.createTextChannel(groupName).queue();
		}else for(Channel channel:category.getChannels()) {
			if(channel.getName().equals(DEFAULT_CHANNEL_NAME)) {
				channel.getManager().setName(groupName).queue();
			}
		}
		message.addReaction(REACTION_JOIN_GROUP).queue();
		message.addReaction(REACTION_ADD_VOICE_CHANNEL).queue();
		message.addReaction(REACTION_ADD_TEXT_CHANNEL).queue();
		groups.put(message.getIdLong(), groupName);
		writeData(groups);
		// System.out.println(groups);
	}

	/**
	 * @param message
	 * 
	 */
	private static void invalidName(Message message) {
		message.addReaction(REACTION_NO).queue();
	}

	/**
	 * @param content
	 * @return
	 */
	private static String getFirstWord(String content) {
		String workValue = content.trim() + " ";
		return workValue.substring(0, workValue.indexOf(' '));
	}

	/**
	 * @param guild
	 * @param guildController
	 * @param groupName
	 * @return
	 */
	private static Role newRole(Guild guild, GuildController guildController, String groupName) {
		for (Role role : guild.getRoles()) {
			if (role.getName().equals(groupName))
				return role;
		}

		return (Role) guildController.createRole().complete();
	}

	/**
	 * @param guild
	 * @param guildController
	 * @param groupName
	 * @return
	 */
	private static Category newCategory(Guild guild, GuildController guildController, String groupName) {

		for (Category category : guild.getCategories()) {
			if (category.getName().equals(groupName)) {
				return findChannels(category);
			}

		}

		return findChannels((Category) guildController.createCategory(groupName).complete());

	}

	private static Category findChannels(Category category) {
		Guild guild = category.getGuild();
		for (TextChannel channel : guild.getTextChannels()) {
			if (channel.getName().equals(category.getName())) {
				for (PermissionOverride permissionOverride : channel.getRolePermissionOverrides()) {
					try {
						category.createPermissionOverride(permissionOverride.getRole())
								.setPermissions(permissionOverride.getAllowed(), permissionOverride.getDenied())
								.complete();
					} catch (Exception e) {

					}
				}
				channel.getManager().setParent(category).complete();
			}
		}
		for (VoiceChannel channel : guild.getVoiceChannels()) {
			if (channel.getName().equals(category.getName())) {
				for (PermissionOverride permissionOverride : channel.getRolePermissionOverrides()) {
					try {
						category.createPermissionOverride(permissionOverride.getRole())
								.setPermissions(permissionOverride.getAllowed(), permissionOverride.getDenied())
								.complete();
					} catch (Exception e) {

					}
				}
				channel.getManager().setParent(category).complete();
			}
		}
		return category;
	}

	/**
	 * @param event
	 */
	private void onJoinNewServer(GuildMemberRoleAddEvent event) {
		for (TextChannel textChannel : event.getGuild().getTextChannels()) {
			if (textChannel.getName().equals(GROUP_CHANNEL_NAME)) {
				askOwner(event, textChannel);
				return;
			} else {
				askOwner(event);
			}
		}

		event.getGuild().getController().createTextChannel(GROUP_CHANNEL_NAME).queue();
		event.getGuild().getOwner().getUser().openPrivateChannel().complete().sendMessage(NEW_GUILD_OWNER_MESSAGE);
	}

	/**
	 * @param event
	 * 
	 */
	private void askOwner(GuildMemberRoleAddEvent event) {
		PrivateChannel ownerChannel = event.getGuild().getOwner().getUser().openPrivateChannel().complete();
		ownerChannel.sendMessage(NEW_GUILD_OWNER_MESSAGE);
		ownerChannel.sendMessage(NEW_GUILD_NO_CHANNEL);
	}

	/**
	 * @param event
	 * @param textChannel
	 */
	private void askOwner(GuildMemberRoleAddEvent event, TextChannel textChannel) {
		textChannel.sendMessage(textChannel.getGuild().getOwner().getAsMention() + " " + NEW_GUILD_OWNER_MESSAGE)
				.queue();
		Message question = textChannel.sendMessage(NEW_GUILD_YES_CHANNEL).complete();
		question.addReaction(REACTION_YES).queue();
		question.addReaction(REACTION_NO).queue();
	}

	private static boolean isAlphanumeric(String str) {
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c < 0x30 || (c >= 0x3a && c <= 0x40) || (c > 0x5a && c <= 0x60) || c > 0x7a)
				return false;
		}
		return true;
	}

	private static String serialize(Object data) {
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			ObjectOutputStream so = new ObjectOutputStream(bo);
			so.writeObject(data);
			so.flush();

			return Base64.getEncoder().encodeToString(bo.toByteArray());
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

	private static Object deserialize(String data) {
		try {
			byte b[] = Base64.getDecoder().decode(data.getBytes());
			ByteArrayInputStream bi = new ByteArrayInputStream(b);
			ObjectInputStream si = new ObjectInputStream(bi);
			return si.readObject();
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
}
