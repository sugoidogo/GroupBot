/**
 * 
 */
package tk.nerdherd.bot;

import net.dv8tion.jda.core.entities.Message;

/**
 * @author root
 *
 */
public interface BotInterface extends Runnable {

	/**
	 * Process a command for this bot
	 * 
	 * @param message
	 *            the source of the command
	 */
	void command(Message message);

	/**
	 * get the alias for this bot
	 * 
	 * @return the alias for this bot
	 */
	abstract String getAlias();

}
