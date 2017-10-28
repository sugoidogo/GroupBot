package tk.nerdherd.bot;

import java.util.Scanner;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;

public class GroupBot extends ListenerAdapter{
	private static final String GROUP_CHANNEL_NAME = "groups";
	private static final String REACTION_ADD_VOICE_CHANNEL = "🎙";
	private static final String REACTION_JOIN_GROUP = "👤";
	private static final String REACTION_ADD_TEXT_CHANNEL = "💬";
	private static final String NEW_GUILD_OWNER_MESSAGE = "This bot requires a channel called 'groups' to perform all of it's functions.";
	private static final String NEW_GUILD_NO_CHANNEL = "Please create a new text channel anywhere in your server by that name and I can get started. Note that there needs to be only one channel by this name or bad things will happen!";
	private static final String NEW_GUILD_YES_CHANNEL = "Is this channel made for this bot?";
	private static final String REACTION_YES = "✅";
	private static final String REACTION_NO = "❎";
	private static final String DEFAULT_CHANNEL_NAME = "general";
	private static JDA jda;

	public static void main(String[] args) {
		
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
	}
	
	@Override
	public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
		if (event.getRoles().get(0).getName().equals(jda.getSelfUser().getName())) {
			onJoinNewServer(event);
		}
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if(event.getChannel().getName().equals(GROUP_CHANNEL_NAME)) {
			newGroup(event.getMessage());
		}
	}
	
	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		//Don't react to reactions from the bot itself
		if(event.getUser().equals(jda.getSelfUser()))return;
		
		//Determine what to do based on the reaction
		String reaction=event.getReactionEmote().getName();
		if(reaction.equals(REACTION_ADD_VOICE_CHANNEL)) addVoiceChannel(event);
		else if(reaction.equals(REACTION_JOIN_GROUP)) joinGroup(event);
		else if(reaction.equals(REACTION_ADD_TEXT_CHANNEL)) addTextChannel(event);
		else if(event.getMember().isOwner() && event.getChannel().getName().equals(GROUP_CHANNEL_NAME)) {
			if (reaction.equals(REACTION_YES)) onJoinNewServer(event,true);
			else if (reaction.equals(REACTION_NO)) onJoinNewServer(event,false);
		}
		
		System.out.println(reaction);
	}

	/**
	 * @param event 
	 * @param b
	 */
	private void onJoinNewServer(GuildMessageReactionAddEvent event, boolean b) {
		if(b) {
			for(Message message : new MessageHistory(event.getChannel()).retrievePast(100).complete()) {
				newGroup(message);
			}
		}else {
			event.getChannel().sendMessage(NEW_GUILD_NO_CHANNEL);
		}
		
	}

	/**
	 * @param event
	 */
	private void addTextChannel(GuildMessageReactionAddEvent event) {
		// TODO
		
	}

	/**
	 * @param event 
	 * 
	 */
	private void addVoiceChannel(GuildMessageReactionAddEvent event) {
		// TODO
		System.out.println("add voice channel to group");
	}

	/**
	 * @param event
	 */
	private void joinGroup(GuildMessageReactionAddEvent event) {
		// TODO
		
	}

	/**
	 * @param message
	 */
	private void newGroup(Message message) {
		// TODO create a group
		if(message.getContent().equals(NEW_GUILD_NO_CHANNEL)) {message.delete().queue(); return;}
		if(message.getContent().equals(NEW_GUILD_YES_CHANNEL)) {message.delete().queue(); return;}
		if(message.getContent().equals(NEW_GUILD_OWNER_MESSAGE)) {message.delete().queue(); return;}
		
		String groupName=getFirstWord(message.getContent());
		Guild guild=message.getGuild();
		GuildController guildController=guild.getController();
		Role role=newRole(guild,guildController,groupName);
		role.getManager().setName(groupName).complete();
		Category category = newCategory(guild,guildController,groupName);
		category.createPermissionOverride(guild.getPublicRole()).setDeny(Permission.MESSAGE_READ).queue();
		category.createPermissionOverride(role).setAllow(Permission.MESSAGE_READ).queue();
		category.createPermissionOverride(message.getMember()).setAllow(Permission.ALL_PERMISSIONS).queue();
		category.createTextChannel(DEFAULT_CHANNEL_NAME).queue();
		
		message.addReaction(REACTION_JOIN_GROUP).queue();;
		message.addReaction(REACTION_ADD_VOICE_CHANNEL).queue();
		message.addReaction(REACTION_ADD_TEXT_CHANNEL).queue();
	}

	/**
	 * @param content
	 * @return
	 */
	private String getFirstWord(String content) {
		String workValue=content.trim()+" ";
		return workValue.substring(0, workValue.indexOf(' '));
	}

	/**
	 * @param guild
	 * @param guildController
	 * @param groupName
	 * @return
	 */
	private Role newRole(Guild guild, GuildController guildController, String groupName) {
		for(Role role : guild.getRoles()) {
			if(role.getName().equals(groupName)) return role;
		}
		
		return (Role) guildController.createRole().complete();
	}

	/**
	 * @param guild 
	 * @param guildController 
	 * @param groupName
	 * @return
	 */
	private Category newCategory(Guild guild, GuildController guildController, String groupName) {

		for(Category category : guild.getCategories()) {
			if(category.getName().equals(groupName)) return category;
		}
		
		return (Category) guildController.createCategory(groupName).complete();
		
	}

	/**
	 * @param event
	 */
	private void onJoinNewServer(GuildMemberRoleAddEvent event) {
		for(TextChannel textChannel : event.getGuild().getTextChannels()) {
			if(textChannel.getName().equals(GROUP_CHANNEL_NAME)) {
				askOwner(event,textChannel);
				return;
			}else {
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
		PrivateChannel ownerChannel=event.getGuild().getOwner().getUser().openPrivateChannel().complete();
		ownerChannel.sendMessage(NEW_GUILD_OWNER_MESSAGE);
		ownerChannel.sendMessage(NEW_GUILD_NO_CHANNEL);
	}

	/**
	 * @param event 
	 * @param textChannel
	 */
	private void askOwner(GuildMemberRoleAddEvent event, TextChannel textChannel) {
		textChannel.sendMessage(textChannel.getGuild().getOwner().getAsMention()+" "+NEW_GUILD_OWNER_MESSAGE).queue();
		Message question=textChannel.sendMessage(NEW_GUILD_YES_CHANNEL).complete();
		question.addReaction(REACTION_YES).queue();
		question.addReaction(REACTION_NO).queue();
	}

}
