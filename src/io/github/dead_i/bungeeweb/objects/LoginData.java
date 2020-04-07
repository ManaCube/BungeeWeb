package io.github.dead_i.bungeeweb.objects;

import lombok.Getter;

/**
 * BungeeWeb - Developed by Lewes D. B. (Boomclaw).
 * All rights reserved 2020.
 */
public class LoginData
{

	@Getter
	private final String id;
	@Getter
	private final String username;
	@Getter
	private final String password;
	@Getter
	private final int    group;

	public LoginData(String id, String username, String password, int group)
	{
		this.id = id;
		this.username = username;
		this.password = password;
		this.group = group;
	}

}
