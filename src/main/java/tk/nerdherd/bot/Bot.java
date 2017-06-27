/**
 * 
 */
package tk.nerdherd.bot;

import java.util.ArrayList;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * @author root
 */
public class Bot implements Runnable {

	private static final String BOT_CHANNEL_NAME = "bot";
	private ArrayList<BotInterface> bots = new ArrayList<BotInterface>();
	private static JDA jda;
	private static String botToken;
	protected Guild guild;
	protected TextChannel botChannel;

	/**
	 * 
	 */
	public Bot(Guild aGuild) {
		guild = aGuild;
		botChannel = guild.getTextChannelsByName(BOT_CHANNEL_NAME, false).get(0);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		botToken = args[0];
		try {
			jda = new JDABuilder(AccountType.BOT).setToken(botToken).buildBlocking();
		} catch (LoginException | IllegalArgumentException | InterruptedException | RateLimitedException e) {
			e.printStackTrace();
		}

		for (Guild guild : jda.getGuilds()) {
			new Thread(new Bot(guild)).start();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		bots.add(new GroupBot(guild));

		jda.addEventListener(new ListenerAdapter() {
			public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
				if (event.getChannel().equals(botChannel)) {
					for (BotInterface bot : bots) {
						if (event.getMessage().getContent().toLowerCase().startsWith(bot.getAlias())) {
							bot.command(event.getMessage());
						}
					}
				}
			}

			public void onGuildLeave(GuildLeaveEvent event) {
				if (event.getGuild().equals(guild)) {
					jda.removeEventListener(this);
				}
			}
		});
	}

}
