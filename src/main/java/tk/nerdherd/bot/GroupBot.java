package tk.nerdherd.bot;

import java.util.HashMap;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;

public class GroupBot extends ListenerAdapter {
	public static final String GROUP_CHANNEL_NAME = "groups";
	private static final String REACTION_JOIN_GROUP = "👤";
	private static final String NEW_GUILD_OWNER_MESSAGE = "This bot requires a channel called 'groups' to perform all of it's functions.";
	private static final String NEW_GUILD_NO_CHANNEL = "Please create a new text channel anywhere in your server by that name and I can get started. Note that there needs to be only one channel by this name or bad things will happen!";
	private static final String NEW_GUILD_YES_CHANNEL = "Is this channel made for this bot?";
	private static final String REACTION_YES = "✅";
	private static final String REACTION_NO = "❎";
	public static JDA jda;
	private static HashMap<Long, String> groups = new HashMap<Long, String>();
	private boolean deleting = false;

	public static void main(String[] args) {

		System.setProperty("org.slf4j.Loggerger.log.WebSocketClient", "trace");
		/*
		 * Attempt to log in to bot account and register our event listener
		 */
		try {
			jda = new JDABuilder(AccountType.BOT).setToken(args[0]).buildBlocking();
			jda.addEventListener(new GroupBot());
			createData();
		} catch (LoginException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void createData() {
		for (Guild guild : jda.getGuilds()) {
			try {
				TextChannel channel = guild.getTextChannelsByName(GROUP_CHANNEL_NAME, true).get(0);
				for (Message message : channel.getIterableHistory().complete()) {
					newGroup(message);

				}
			} catch (Exception e) {

			}
		}
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
		if (reaction.equals(REACTION_JOIN_GROUP))
			joinGroup(event);
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
		guild.getRolesByName(groups.get(messageIdLong), true).get(0).delete().queue();
		groups.remove(messageIdLong);
		deleting = false;
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
	private void joinGroup(GuildMessageReactionAddEvent event) {
		String group = getFirstWord(
				event.getChannel().getMessageById(event.getMessageId()).complete().getContentStripped());
		Guild guild = event.getGuild();
		guild.getController().addRolesToMember(event.getMember(), guild.getRolesByName(group, true).get(0)).queue();
	}

	/**
	 * @param message
	 */
	private static void newGroup(Message message) {
		if (message.getContentStripped().equals(NEW_GUILD_NO_CHANNEL)) {
			message.delete().queue();
			return;
		}
		if (message.getContentStripped().equals(NEW_GUILD_YES_CHANNEL)) {
			message.delete().queue();
			return;
		}
		if (message.getContentStripped().equals(NEW_GUILD_OWNER_MESSAGE)) {
			message.delete().queue();
			return;
		}

		String groupName = getFirstWord(message.getContentStripped());
		if (groups.containsValue(groupName)) {
			message.delete().queue();
			return;
		}
		Guild guild = message.getGuild();
		GuildController guildController = guild.getController();

		try {
			Role role = newRole(guild, guildController, groupName);
			role.getManager().setName(groupName).queue();
			role.getManager().setMentionable(true);
			for (User mentionedUser : message.getMentionedUsers()) {
				guildController.addRolesToMember(guild.getMember(mentionedUser), role).queue();
			}
			guildController.addRolesToMember(message.getMember(), role);
			message.addReaction(REACTION_JOIN_GROUP).queue();
		} catch (Exception e) {
			invalidName(message);
			return;
		}
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
}
