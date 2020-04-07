package io.github.dead_i.bungeeweb;

import com.ayanix.panther.impl.common.storage.sql.HikariMySQLStorage;
import com.ayanix.panther.internal.sql2o.Connection;
import com.ayanix.panther.internal.sql2o.Query;
import com.ayanix.panther.internal.sql2o.ResultSetHandler;
import lombok.Getter;

import java.util.logging.Level;

public class DatabaseManager
{

    @Getter
	private HikariMySQLStorage storage;
	private BungeeWeb          plugin;
	private String             host;
	private int                port;
	private String             database;
	private String             user;
	private String             pass;

	public DatabaseManager(BungeeWeb plugin, String host, int port, String database, String user, String pass)
	{
		this.plugin = plugin;
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.pass = pass;
	}

	public boolean connect()
	{
		HikariMySQLStorage.HikariSQLStorageBuilder builder = new HikariMySQLStorage.HikariSQLStorageBuilder();

		storage = builder.host(host)
				.port(port)
				.username(user)
				.password(pass)
				.database(database)
				.build();

		try
		{
			storage = builder.build();
		} catch (Exception e)
		{
			plugin.getLogger().log(Level.SEVERE, "Unable to connect to database", e);
			storage = null;
			return false;
		}

		// Initial database table setup
		try (Connection connection = storage.getSQL().open())
		{
			try (Query query = connection.createQuery("CREATE TABLE IF NOT EXISTS `" + BungeeWeb.getConfig().getString("database.prefix") + "log` (`id` int" +
                    "(16) NOT NULL AUTO_INCREMENT, `time` int(10) NOT NULL, `type` int(2) NOT NULL, `uuid` varchar(32) NOT NULL, `username` varchar(16) NOT " +
                    "NULL, `content` varchar(100) NOT NULL DEFAULT '', PRIMARY KEY (`id`)) CHARACTER SET utf8"))
			{
				query.executeUpdate();
			}

			try (Query query = connection.createQuery("CREATE TABLE IF NOT EXISTS `" + BungeeWeb.getConfig().getString("database.prefix") + "users` (`id` int" +
                    "(4) NOT NULL AUTO_INCREMENT, `user` varchar(16) NOT NULL, `pass` varchar(32) NOT NULL, `salt` varchar(16) NOT NULL, `group` int(1) NOT " +
                    "NULL DEFAULT '1', PRIMARY KEY (`id`)) CHARACTER SET utf8"))
			{
				query.executeUpdate();
			}

			try (Query query = connection.createQuery("CREATE TABLE IF NOT EXISTS `" + BungeeWeb.getConfig().getString("database.prefix") + "stats` (`id` int" +
                    "(16) NOT NULL AUTO_INCREMENT, `time` int(10) NOT NULL, `playercount` int(6) NOT NULL DEFAULT -1, `maxplayers` int(6) NOT NULL DEFAULT " +
                    "-1, `activity` int(12) NOT NULL DEFAULT -1, PRIMARY KEY (`id`)) CHARACTER SET utf8"))
			{
				query.executeUpdate();
			}

			try (Query query = connection.createQuery("SELECT COUNT(*) FROM `" + BungeeWeb.getConfig().getString("database.prefix") + "users`"))
			{
				query.executeAndFetch((ResultSetHandler<Object>) resultSet -> {
					if (resultSet.getInt(1) == 0)
					{
						String salt = BungeeWeb.salt();

						try (Query insertQuery = connection.createQuery("INSERT INTO `" + BungeeWeb.getConfig().getString("database.prefix") + "users` " +
                                "(`user`, `pass`, `salt`, `group`) VALUES('admin', '" + BungeeWeb.encrypt("admin", salt) + "', '" + salt + "', 3)"))
						{
							insertQuery.executeUpdate();

							plugin.getLogger().warning("A new admin account has been created.");
							plugin.getLogger().warning("Both the username and password is 'admin'. Please change the password after first logging in.");
						}
					}

					return null;
				});
			}
		}

		return true;
	}

}
