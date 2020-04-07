package io.github.dead_i.bungeeweb.api;

import com.ayanix.panther.internal.sql2o.Connection;
import com.ayanix.panther.internal.sql2o.Query;
import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class CreateUser extends APICommand
{
	public CreateUser()
	{
		super("createuser", "settings.users.create");
	}

	@Override
	public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException
	{
		String user  = req.getParameter("user");
		String pass  = req.getParameter("pass");
		String group = req.getParameter("group");
		String salt  = BungeeWeb.salt();

		if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty() && group != null && BungeeWeb.isNumber(group))
		{
			if (user.length() <= 16)
			{
				int groupid = Integer.parseInt(group);

				if (groupid < BungeeWeb.getGroupPower(req))
				{
					try (Connection connection = BungeeWeb.getManager().getStorage().getSQL().open();
					     Query query = connection.createQuery("INSERT INTO `" + BungeeWeb.getConfig().getString("database.prefix") + "users` (`user`, `pass`, " +
                                 "`salt`, `group`) VALUES(:user, :pass, :salt, :group)"))
					{
						query.addParameter("user", user)
								.addParameter("pass", BungeeWeb.encrypt(pass, salt))
								.addParameter("salt", salt)
								.addParameter("group", groupid)
								.executeUpdate();
					}

					res.getWriter().print("{ \"status\": 1 }");
				} else
				{
					res.getWriter().print("{ \"status\": 0, \"error\": \"You do not have permission to create a user of this group.\" }");
				}
			} else
			{
				res.getWriter().print("{ \"status\": 0, \"error\": \"The username provided is too long.\" }");
			}
		} else
		{
			res.getWriter().print("{ \"status\": 0, \"error\": \"Incorrect usage.\" }");
		}
	}
}
