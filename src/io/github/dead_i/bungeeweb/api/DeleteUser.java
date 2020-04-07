package io.github.dead_i.bungeeweb.api;

import com.ayanix.panther.internal.sql2o.Connection;
import com.ayanix.panther.internal.sql2o.Query;
import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DeleteUser extends APICommand
{
	public DeleteUser()
	{
		super("deleteuser", "settings.users.delete");
	}

	@Override
	public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException
	{
		String id = req.getParameter("id");

		if (id != null && !id.isEmpty() && BungeeWeb.isNumber(id))
		{
			try (Connection connection = BungeeWeb.getManager().getStorage().getSQL().open();
			     Query query = connection.createQuery("DELETE FROM `" + BungeeWeb.getConfig().getString("database.prefix") + "users` WHERE `id`=:id AND " +
                         "`group`<:group"))
			{
				query.addParameter("id", Integer.parseInt(id))
						.addParameter("group", BungeeWeb.getGroupPower(req))
						.executeUpdate();
			}

			res.getWriter().print("{ \"status\": 1 }");
		} else
		{
			res.getWriter().print("{ \"status\": 0, \"error\": \"Incorrect usage.\" }");
		}
	}
}
