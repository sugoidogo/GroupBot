/**
 * 
 */
package tk.nerdherd.bot;

import java.util.ArrayList;
import java.util.Scanner;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.managers.GuildController;

/**
 * @author root
 *
 */
public class GroupBot extends Bot implements BotInterface {

	private final String HELP_TEXT = "commands:\ncreate - create a new group\njoin - join a group\nleave - leave a group\ndelete - delete a group";
	private final String LEAVE = "leave";
	private final String ALIAS = "group";
	private final String GROUP_CHANNEL_NAME = "groups";
	private final String CREATE = "create";
	private final String JOIN = "join";
	private final String DELETE = "delete";
	private final int MESSAGE_ID = 0;
	private final int ROLE_NAME = 1;
	private final int ROLE_ID = 2;
	private final int NOTHING = 0;
	private TextChannel groupChannel;
	private GuildController guildController;
	private ArrayList<ArrayList<String>> groups = new ArrayList<>();

	/**
	 * @param aGuild
	 */
	public GroupBot(Guild aGuild) {
		super(aGuild);
		guildController = guild.getController();

		try {
			groupChannel = guild.getTextChannelsByName(GROUP_CHANNEL_NAME, false).get(0);
		} catch (Exception e) {
			groupChannel = (TextChannel) guildController.createTextChannel(GROUP_CHANNEL_NAME)
					.addPermissionOverride(guild.getPublicRole(), Permission.MESSAGE_READ.getRawValue(),
							Permission.MESSAGE_WRITE.getRawValue())
					.complete();
		}

		for (Message message : groupChannel.getHistory().retrievePast(100).complete()) {
			Scanner in = new Scanner(message.getContent());
			ArrayList<String> group = new ArrayList<String>();
			group.add(message.getId());
			while (in.hasNext()) {
				group.add(in.next());
			}
			groups.add(group);
			in.close();
		}

		System.out.println(groups);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tk.nerdherd.bot.BotInterface#command(net.dv8tion.jda.core.entities.
	 * Message)
	 */
	@Override
	public void command(Message message) {
		Scanner in = new Scanner(message.getContent());
		in.next();
		String cmd = in.next();
		String arg = in.nextLine().trim();
		in.close();
		if (arg.matches("[^a-zA-Z0-9]"))
			return;

		if (cmd.equals(CREATE)) {
			create(arg, message.getMember());
		} else if (cmd.equals(JOIN)) {
			join(arg, message.getMember());
		} else if (cmd.equals(LEAVE)) {
			leave(arg, message.getMember());
		} else if (cmd.equals(DELETE)) {
			delete(arg, message.getMember());
		} else {
			help();
		}
	}

	/**
	 * 
	 */
	private void help() {
		botChannel.sendMessage(HELP_TEXT);

	}

	/**
	 * @param arg
	 * @param member
	 */
	private void delete(String arg, Member member) {
		if (member.hasPermission(Permission.MANAGE_CHANNEL)) {
			for (ArrayList<String> group : groups) {
				if (group.get(ROLE_NAME).equals(arg)) {
					guild.getRoleById(group.get(ROLE_ID)).delete().queue();
					for (String channelId : group.subList(3, group.size())) {
						try {
							guild.getTextChannelById(channelId).delete().queue();
						} catch (Exception e) {
							guild.getVoiceChannelById(channelId).delete().queue();
						}
					}
					groups.remove(group);
					groupChannel.getMessageById(group.get(MESSAGE_ID)).complete().delete().queue();
					return;
				}
			}
		}
	}

	/**
	 * @param arg
	 * @param member
	 */
	private void leave(String arg, Member member) {
		for (ArrayList<String> group : groups) {
			if (group.get(ROLE_NAME).equals(arg)) {
				guildController.removeRolesFromMember(member, guild.getRoleById(group.get(ROLE_ID))).queue();
			}
		}

	}

	/**
	 * @param arg
	 * @param member
	 */
	@SuppressWarnings("serial")
	private void create(String arg, Member member) {
		Role role = guildController.createRole().setName(arg).complete();
		TextChannel channel = (TextChannel) guildController.createTextChannel(arg)
				.addPermissionOverride(guild.getPublicRole(), NOTHING, Permission.MESSAGE_READ.getRawValue())
				.addPermissionOverride(role, Permission.MESSAGE_READ.getRawValue(), NOTHING).complete();

		groups.add(new ArrayList<String>() {
			{
				add(groupChannel.sendMessage(role.getName() + " " + role.getId() + " " + channel.getId()).complete()
						.getId());
				add(role.getName());
				add(role.getId());
				add(channel.getId());
			}
		});

		join(role.getName(), member);
	}

	/**
	 * @param arg
	 * @param member
	 * @param author
	 */
	private void join(String arg, Member member) {
		for (ArrayList<String> group : groups) {
			if (group.get(ROLE_NAME).equals(arg)) {
				guildController.addRolesToMember(member, guild.getRoleById(group.get(ROLE_ID))).queue();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tk.nerdherd.bot.BotInterface#getAlias()
	 */
	@Override
	public String getAlias() {
		return ALIAS;
	}

}
