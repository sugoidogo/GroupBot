/**
 *
 */
package tk.nerdherd.bot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.requests.SessionReconnectQueue;

/**
 * @author josephsmendoza
 *
 */
@SuppressWarnings("unchecked")
public class GroupBotSharded extends ListenerAdapter {

	private static final String REACTION_ADD_VOICE_CHANNEL = "🎙";
	private static final String REACTION_JOIN_GROUP = "👤";
	private static final String REACTION_ADD_TEXT_CHANNEL = "💬";
	private static final String NEW_GUILD_OWNER_MESSAGE = "This bot requires a channel called 'groups' to perform all of it's functions.";
	private static final String NEW_GUILD_NO_CHANNEL = "Please create a new text channel anywhere in your server by that name and I can get started. Note that there needs to be only one channel by this name or bad things will happen!";
	private static final String NEW_GUILD_YES_CHANNEL = "Is this channel made for this bot?";
	private static final String REACTION_YES = "✅";
	private static final String REACTION_NO = "❎";
	private static final String DEFAULT_CHANNEL_NAME = "general";
	private static final String GROUP_CHANNEL_NAME = "groups";
	private static final String DATA_CHANNEL_NAME = "nerdbotdata";
	private static int guilds;
	private JDA jda;
	private Guild guild;
	private GuildController guildController;
	private TextChannel groupChannel;
	private TextChannel dataChannel;
	private Map<Long, Message> messages;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO
		JDABuilder shardBuilder = new JDABuilder(AccountType.BOT).setToken(args[0])
				.setReconnectQueue(new SessionReconnectQueue());
		try {
			JDA temp = shardBuilder.buildBlocking();
			guilds = temp.getGuilds().size();
			temp.shutdown();
			System.out.println("Shutdown of initial login complete");
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

		for (int i = 0; i < guilds; i++) {
			try {
				System.out.println("Waiting 5 seconds between connection attempts");
				Thread.sleep(5000);
				new GroupBotSharded(shardBuilder.useSharding(i, guilds).buildBlocking());
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
		}
	}

	/**
	 * @param jda
	 * 
	 */
	public GroupBotSharded(JDA shard) {
		// TODO
		jda = shard;
		guild = jda.getGuilds().get(0);
		guildController = guild.getController();
		try {
			groupChannel = guild.getTextChannelsByName(GROUP_CHANNEL_NAME, true).get(0);
		} catch (Exception e) {
			groupChannel = (TextChannel) guildController.createTextChannel(GROUP_CHANNEL_NAME).complete();
		}
		try {
			dataChannel = guild.getTextChannelsByName(DATA_CHANNEL_NAME, true).get(0);
		} catch (Exception e) {
			dataChannel = (TextChannel) guildController.createTextChannel(DATA_CHANNEL_NAME).complete();
		}
		try {
			messages = (Map<Long, Message>) deserialize(
					dataChannel.getMessageById(dataChannel.getLatestMessageIdLong()).complete().getContent());
		} catch (Exception e) {
			messages = importData();
			// verify();
		}
		// sync();
		jda.addEventListener(this);
		System.out.println("Init Complete!");
	}

	/**
	 * 
	 */
	private void verify() {
		// TODO

	}

	/**
	 * 
	 */
	private void sync() {
		Map<Long, Message> importedData = importData();
		for (Long key : importedData.keySet()) {
			if (messages.containsKey(key)) {
				if (!importedData.get(key).equals(messages.get(key))) {
					Message newMessage = importedData.get(key);
					Message oldMessage = messages.get(key);
					String newGroupName = getFirstWord(newMessage.getContent()).trim();
					String oldGroupName = getFirstWord(oldMessage.getContent()).trim();
					if (!newGroupName.equals(oldGroupName)) {
						renameGroup(oldGroupName, newGroupName);
					}
					List<MessageReaction> oldReactions = oldMessage.getReactions();
					List<MessageReaction> newReactions = newMessage.getReactions();
					for (int i = 0; i < newReactions.size(); i++) {
						if (!oldReactions.contains(newReactions.get(i))) {

						}
					}
				}
			} else {
				newGroup(importedData.get(key));
			}
		}
		for (Long key : messages.keySet()) {
			if (!importedData.containsKey(key)) {
				deleteGroup(key);
			}
		}
	}

	/**
	 * @param oldGroupName
	 * @param newGroupName
	 */
	private void renameGroup(String oldGroupName, String newGroupName) {
		// TODO

	}

	/**
	 * @param messageID
	 */
	private void deleteGroup(Long messageID) {
		String groupName = getFirstWord(messages.get(messageID).getContent()).trim();
		guild.getRolesByName(groupName, true).get(0).delete().queue();
		Category group = guild.getCategoriesByName(groupName, true).get(0);
		for (Channel channel : group.getChannels()) {
			channel.delete().queue();
		}
		group.delete().queue();
		messages.remove(messageID);
		update();
	}

	/**
	 * @return
	 * 
	 */
	private Map<Long, Message> importData() {
		Map<Long, Message> data = new HashMap<Long, Message>();
		for (Message message : new MessageHistory(groupChannel).retrievePast(100).complete()) {
			data.put(message.getIdLong(), message);
		}
		return data;
	}

	@Override
	public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
		if (!event.getChannel().equals(groupChannel))
			return;

		deleteGroup(event.getMessageIdLong());
	}

	@Override
	public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
		// TODO
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getChannel().equals(groupChannel)) {
			newGroup(event.getMessage());
		}
	}

	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		// Don't react outside of the groups channel
		if (!event.getChannel().equals(groupChannel))
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
	}

	private void newGroup(Message message) {
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
		if (!isAlphanumeric(groupName.trim())) {
			invalidName(message);
			return;
		}
		Role role = newRole(groupName);
		role.getManager().setName(groupName).queue();
		Category category = newCategory(groupName);
		category.createPermissionOverride(guild.getPublicRole()).setDeny(Permission.MESSAGE_READ).queue();
		category.createPermissionOverride(role).setAllow(Permission.MESSAGE_READ).queue();
		category.createPermissionOverride(message.getMember()).setAllow(Permission.ALL_PERMISSIONS).queue();
		for (Role mentionedRole : message.getMentionedRoles()) {
			category.createPermissionOverride(mentionedRole).setAllow(Permission.MESSAGE_READ).queue();
		}
		for (User mentionedUser : message.getMentionedUsers()) {
			guildController.addRolesToMember(guild.getMember(mentionedUser), role).queue();
		}
		category.createTextChannel(DEFAULT_CHANNEL_NAME).queue();

		message.addReaction(REACTION_JOIN_GROUP).queue();
		message.addReaction(REACTION_ADD_VOICE_CHANNEL).queue();
		message.addReaction(REACTION_ADD_TEXT_CHANNEL).queue();

		messages.put(message.getIdLong(), message);
		update();
	}

	/**
	 * 
	 */
	private void update() {
		try {
			dataChannel.getMessageById(dataChannel.getLatestMessageId()).complete().delete().queue();
		} finally {
		}
		dataChannel.sendMessage(serialize(messages)).queue();
		System.out.println(serialize(messages));
	}

	/**
	 * @param guild
	 * @param guildController
	 * @param groupName
	 * @return
	 */
	private Role newRole(String groupName) {
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
	private Category newCategory(String groupName) {

		for (Category category : guild.getCategories()) {
			if (category.getName().equals(groupName))
				return category;
		}

		return (Category) guildController.createCategory(groupName).complete();

	}

	private String getFirstWord(String content) {
		String workValue = content.trim() + " ";
		return workValue.substring(0, workValue.indexOf(' '));
	}

	private boolean isAlphanumeric(String str) {
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c < 0x30 || (c >= 0x3a && c <= 0x40) || (c > 0x5a && c <= 0x60) || c > 0x7a)
				return false;
		}
		return true;
	}

	/**
	 * @param message
	 * 
	 */
	private void invalidName(Message message) {
		message.addReaction(REACTION_NO).queue();
	}

	/**
	 * @param event
	 */
	private void joinGroup(GuildMessageReactionAddEvent event) {
		String group = getFirstWord(messages.get(event.getMessageIdLong()).getContent());
		guildController.addRolesToMember(event.getMember(), guild.getRolesByName(group, true).get(0)).queue();
	}

	/**
	 * @param event
	 * 
	 */
	private void addVoiceChannel(GuildMessageReactionAddEvent event) {
		Message message = messages.get(event.getMessageIdLong());
		if (!event.getMember().equals(message.getMember()))
			return;
		guild.getCategoriesByName(getFirstWord(message.getContent()), true).get(0)
				.createVoiceChannel(DEFAULT_CHANNEL_NAME).queue();
		event.getReaction().removeReaction(event.getUser()).queue();
	}

	/**
	 * @param event
	 */
	private void addTextChannel(GuildMessageReactionAddEvent event) {
		Message message = messages.get(event.getMessageIdLong());
		if (!event.getMember().equals(message.getMember()))
			return;
		guild.getCategoriesByName(getFirstWord(message.getContent()), true).get(0)
				.createTextChannel(DEFAULT_CHANNEL_NAME).queue();
		event.getReaction().removeReaction(event.getUser()).queue();
	}

	private String serialize(Object data) {
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			ObjectOutputStream so = new ObjectOutputStream(bo);
			so.writeObject(data);
			so.flush();
			return bo.toString();
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

	private Object deserialize(String data) {
		try {
			byte b[] = data.getBytes();
			ByteArrayInputStream bi = new ByteArrayInputStream(b);
			ObjectInputStream si = new ObjectInputStream(bi);
			return si.readObject();
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
}
