package io.github.dead_i.bungeeweb.api;

import com.ayanix.panther.internal.sql2o.Connection;
import com.ayanix.panther.internal.sql2o.Query;
import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ChangePassword extends APICommand
{
	public ChangePassword()
	{
		super("changepassword", "settings.password");
	}

	@Override
	public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws Exception
	{
		String current = req.getParameter("currentpass");
		String pass    = req.getParameter("newpass");
		String confirm = req.getParameter("confirmpass");

		if (current != null &&
				pass != null &&
				pass.equals(confirm) &&
				BungeeWeb.getLogin((String) req.getSession().getAttribute("user"), current) != null)
		{
			try (Connection connection = BungeeWeb.getManager().getStorage().getSQL().open();
			     Query query = connection.createQuery("UPDATE `" + BungeeWeb.getConfig().getString("database.prefix") + "users` SET " +
					     "`pass`=:pass, `salt`=:salt WHERE `id`=:id"))
			{
				String salt = BungeeWeb.salt();

				query.addParameter("pass", BungeeWeb.encrypt(req.getParameter("newpass"), salt))
						.addParameter("salt", salt)
						.addParameter("id", Integer.parseInt((String) req.getSession().getAttribute("id")))
						.executeUpdate();

				res.getWriter().print("{ \"status\": 1 }");
			}
		} else
		{
			res.getWriter().print("{ \"status\": 0 }");
		}
	}
}
