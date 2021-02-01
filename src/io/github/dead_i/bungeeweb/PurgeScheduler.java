package io.github.dead_i.bungeeweb;

import com.ayanix.panther.internal.sql2o.Connection;
import com.ayanix.panther.internal.sql2o.Query;
import com.ayanix.panther.internal.sql2o.ResultSetHandler;
import net.md_5.bungee.api.ProxyServer;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PurgeScheduler implements Runnable
{
	private final BungeeWeb plugin;
	private       String    table;
	private       int       time;
	private       int       min = -1;
	private       boolean   run;

	public PurgeScheduler(BungeeWeb plugin, String table, int days)
	{
		this.plugin = plugin;
		this.table = BungeeWeb.getConfig().getString("database.prefix") + table;
		time = days * 86400;

		plugin.getLogger().info("Deleting " + table + " older than " + days + " days from " + this.table);

		try (Connection connection = BungeeWeb.getManager().getStorage().getSQL().open();
		     Query query = connection.createQuery("SELECT MIN(`id`) FROM `" + this.table + "`"))
		{
			List<Integer> results = query.executeAndFetch((ResultSetHandler<Integer>) resultSet -> resultSet.getInt(1));

			if (!results.isEmpty())
			{
				min = results.get(0);
			}
		}

		run = min > -1;
	}


	@Override
	public void run()
	{
		if (!run)
		{
			return;
		}

		ProxyServer.getInstance().getScheduler().schedule(plugin, () -> BungeeWeb.getExecutor().execute(this::purge), 0L, 500, TimeUnit.MILLISECONDS);
	}

	public void purge()
	{
		// Chunking method courtesy of guidance from http://mysql.rjweb.org/doc.php/deletebig
		try (Connection connection = BungeeWeb.getManager().getStorage().getSQL().open();
		     Query query = connection.createQuery("SELECT `id` FROM `" + table + "` WHERE `id`>=" + min + " ORDER BY `id` LIMIT 1000,1"))
		{
			if (query.executeAndFetch((ResultSetHandler<Object>) resultSet -> {
				int max = resultSet.getInt("id");

				try (Query newQuery = connection.createQuery("DELETE FROM `" + table + "` WHERE `id`>=" + min + " AND `id`<" + max + " AND `time`<" + ((System.currentTimeMillis() / 1000) - time)))
				{
					newQuery.executeUpdate();
				}

				min = max;

				return null;
			}).isEmpty())
			{
				run = false;
			}
		}
	}
}
