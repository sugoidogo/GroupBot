/**
 *
 */
package tk.nerdherd.bot;

import java.io.Serializable;

/**
 * @author josephsmendoza
 *
 */
public class Group implements Serializable {

	private String name;

	/**
	 * 
	 */
	public Group() {
	}
	
	public Group(String groupName) {
		name=groupName;
	}

}
