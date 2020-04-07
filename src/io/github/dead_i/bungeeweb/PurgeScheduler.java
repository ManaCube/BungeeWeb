package io.github.dead_i.bungeeweb;

import com.ayanix.panther.internal.sql2o.Connection;
import com.ayanix.panther.internal.sql2o.Query;
import com.ayanix.panther.internal.sql2o.ResultSetHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PurgeScheduler implements Runnable
{
	private String table;
	private int    time;
	private int    min;

	public PurgeScheduler(String table, int days)
	{
		this.table = BungeeWeb.getConfig().getString("database.prefix") + table;
		time = days * 86400;

		try (Connection connection = BungeeWeb.getManager().getStorage().getSQL().open();
		     Query query = connection.createQuery("SELECT MIN(`id`) FROM `" + this.table + "`"))
		{
			List<Integer> results = query.executeAndFetch((ResultSetHandler<Integer>) resultSet -> resultSet.getInt(1));

			if (!results.isEmpty())
			{
				min = results.get(0);
			}
		}
	}

	@Override
	public void run()
	{
		// Chunking method courtesy of guidance from http://mysql.rjweb.org/doc.php/deletebig
		try (Connection connection = BungeeWeb.getManager().getStorage().getSQL().open();
		     Query query = connection.createQuery("SELECT `id` FROM `" + table + "` WHERE `id`>=" + min + " ORDER BY `id` LIMIT 1000,1"))
		{
			query.executeAndFetch((ResultSetHandler<Object>) resultSet -> {
                int max = resultSet.getInt("id");

                try (Query newQuery =
                             connection.createQuery("DELETE FROM `" + table + "` WHERE `id`>=" + min + " AND `id`<" + max + " AND `time`<" + ((System.currentTimeMillis() / 1000) - time)))
                {
                    newQuery.executeUpdate();
                }

                min = max;

                return null;
            });
		}
	}
}
