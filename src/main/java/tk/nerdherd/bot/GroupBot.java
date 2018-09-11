package tk.nerdherd.bot;

import java.util.HashMap;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.managers.RoleManagerUpdatable;

public class GroupBot extends ListenerAdapter {
	public static final String GROUP_CHANNEL_NAME = "groups";
	public static JDA jda;
	private static HashMap<Long, String> groups = new HashMap<>();
	private static final String REACTION_JOIN_GROUP = "👤";
	private static final String REACTION_NO = "❎";

	public static void main(final String[] args) {

		System.setProperty("org.slf4j.Loggerger.log.WebSocketClient", "trace");
		if(args.length==0) {
			System.out.println("Missing Bot Token!");
			System.exit(1);
		}
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
		for (final Guild guild : jda.getGuilds()) {
			try {
				final TextChannel channel = guild.getTextChannelsByName(GROUP_CHANNEL_NAME, true).get(0);
				for (final Message message : channel.getIterableHistory().complete()) {
					newGroup(message);

				}
			} catch (final Exception e) {

			}
		}
	}

	/**
	 * @param content
	 * @return
	 */
	private static String getFirstWord(final String content) {
		final String workValue = content.trim() + " ";
		return workValue.substring(0, workValue.indexOf(' '));
	}

	/**
	 * @param message
	 * 
	 */
	private static void invalidName(final Message message) {
		message.addReaction(REACTION_NO).queue();
	}

	/**
	 * @param message
	 */
	private static void newGroup(final Message message) {
		final String groupName = getFirstWord(message.getContentStripped());
		if (groups.containsValue(groupName)) {
			message.delete().queue();
			return;
		}
		final Guild guild = message.getGuild();
		final GuildController guildController = guild.getController();
		try {
			final Role role = newRole(guild, guildController, groupName);
			final RoleManagerUpdatable roleMU = role.getManagerUpdatable();
			roleMU.getNameField().setValue(groupName);
			roleMU.getMentionableField().setValue(true);
			roleMU.update().queue();
			for (final User mentionedUser : message.getMentionedUsers()) {
				guildController.addRolesToMember(guild.getMember(mentionedUser), role).queue();
			}
			guildController.addRolesToMember(message.getMember(), role).queue();
			message.addReaction(REACTION_JOIN_GROUP).queue();
		} catch (final Exception e) {
			invalidName(message);
			return;
		}
	}

	/**
	 * @param guild
	 * @param guildController
	 * @param groupName
	 * @return
	 */
	private static Role newRole(final Guild guild, final GuildController guildController, final String groupName) {
		for (final Role role : guild.getRoles()) {
			if (role.getName().equals(groupName)) {
				return role;
			}
		}

		return guildController.createRole().complete();
	}

	@Override
	public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent event) {
		if (event.getRoles().get(0).getName().equals(jda.getSelfUser().getName())) {
			onJoinNewServer(event);
		}
	}

	@Override
	public void onGuildMessageDelete(final GuildMessageDeleteEvent event) {
		if (event.getChannel().getName().equals(GROUP_CHANNEL_NAME) && groups.containsKey(event.getMessageIdLong())) {
			deleteGroup(event.getMessageIdLong(), event.getGuild());
		}
	}

	@Override
	public void onGuildMessageReactionAdd(final GuildMessageReactionAddEvent event) {
		// Don't react outside of the groups channel
		if (!event.getChannel().getName().equals(GROUP_CHANNEL_NAME)) {
			return;
		}

		// Don't react to reactions from the bot itself
		if (event.getUser().equals(jda.getSelfUser())) {
			return;
		}

		// Determine what to do based on the reaction
		final String reaction = event.getReactionEmote().getName();
		if (reaction.equals(REACTION_JOIN_GROUP)) {
			joinGroup(event);
		} else {
			event.getReaction().removeReaction().queue();
		}
	}

	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		if (event.getChannel().getName().equals(GROUP_CHANNEL_NAME)) {
			newGroup(event.getMessage());
		}
	}

	private void deleteGroup(final long messageIdLong, final Guild guild) {
		guild.getRolesByName(groups.get(messageIdLong), true).get(0).delete().queue();
		groups.remove(messageIdLong);
	}

	/**
	 * @param event
	 */
	private void joinGroup(final GuildMessageReactionAddEvent event) {
		final String group = getFirstWord(
				event.getChannel().getMessageById(event.getMessageId()).complete().getContentStripped());
		final Guild guild = event.getGuild();
		guild.getController().addRolesToMember(event.getMember(), guild.getRolesByName(group, true).get(0)).queue();
	}

	/**
	 * @param event
	 */
	private void onJoinNewServer(final GuildMemberRoleAddEvent event) {
		if (event.getGuild().getTextChannelsByName(GROUP_CHANNEL_NAME, true).size() > 0) {
			return;
		}

		event.getGuild().getController().createTextChannel(GROUP_CHANNEL_NAME).queue();
	}
}
