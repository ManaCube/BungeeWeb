package io.github.dead_i.bungeeweb;

import com.ayanix.panther.internal.sql2o.Connection;
import com.ayanix.panther.internal.sql2o.Query;
import com.ayanix.panther.internal.sql2o.ResultSetHandler;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.dead_i.bungeeweb.commands.ReloadConfig;
import io.github.dead_i.bungeeweb.listeners.*;
import io.github.dead_i.bungeeweb.objects.LoginData;
import lombok.Getter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.security.Password;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BungeeWeb extends Plugin {
    private static Configuration config;
    private static Configuration defaultConfig;
    @Getter
    private static DatabaseManager manager;

    @Getter
    private static ExecutorService executor;

    public void onEnable() {
        // Executor service
        // 2 threads minimum as one thread is always used for the web service
        executor = Executors.newFixedThreadPool(5, new ThreadFactoryBuilder().setNameFormat("BungeeWeb %d").build());

        // Get configuration
        reloadConfig(this);

        // Setup locales
        setupDirectory("lang");
        setupLocale("en");
        setupLocale("fr");
        setupLocale("es");
        setupLocale("de");
        setupLocale("it");

        // Setup directories
        setupDirectory("themes");

        // Connect to the database
        manager = new DatabaseManager(this,
                getConfig().getString("database.host"),
                getConfig().getInt("database.port"),
                getConfig().getString("database.db"),
                getConfig().getString("database.user"),
                getConfig().getString("database.pass"));

        if (!manager.connect())
        {
            return;
        }

        // Start automatic chunking
        setupPurging("logs");
        setupPurging("stats");

        // Register listeners
        getProxy().getPluginManager().registerListener(this, new ChatListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerDisconnectListener(this));
        getProxy().getPluginManager().registerListener(this, new PostLoginListener(this));
        getProxy().getPluginManager().registerListener(this, new ServerConnectedListener(this));
        getProxy().getPluginManager().registerListener(this, new ServerKickListener(this));

        // Register commands
        getProxy().getPluginManager().registerCommand(this, new ReloadConfig(this));

        // Graph loops
        int inc = getConfig().getInt("server.statscheck");
        if (inc > 0) getProxy().getScheduler().schedule(this, new Runnable()
        {
            StatusCheck statusCheck = new StatusCheck(BungeeWeb.this, inc);

            @Override
            public void run()
            {
                executor.execute(statusCheck);
            }
        }, inc, inc, TimeUnit.SECONDS);

        // Setup logging
        org.eclipse.jetty.util.log.Log.setLog(new JettyLogger());
        Properties p = new Properties();
        p.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        StdErrLog.setProperties(p);

        // Setup the context
        ContextHandler context = new ContextHandler("/");
        SessionHandler sessions = new SessionHandler(new HashSessionManager());
        sessions.setHandler(new WebHandler(this));
        context.setHandler(sessions);

        // Setup the server
        final Server server = new Server(getConfig().getInt("server.port"));
        server.setSessionIdManager(new HashSessionIdManager());
        server.setHandler(sessions);
        server.setStopAtShutdown(true);

        // Start listening
        executor.execute(() -> {
            try {
                server.start();
            } catch(Exception e) {
                getLogger().warning("Unable to bind web server to port.");
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDisable()
    {
        for (Runnable runnable : executor.shutdownNow())
        {
            runnable.run();
        }

        if(manager.getStorage() != null) {
            manager.getStorage().disconnect();
        }
    }

    public void setupLocale(String lang) {
        try {
            String filename = "lang/" + lang + ".json";
            File file = new File(getDataFolder(), filename);
            if (!file.exists()) file.createNewFile();
            ByteStreams.copy(getResourceAsStream(filename), new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupDirectory(String directory) {
        File dir = new File(getDataFolder(), directory);
        try {
            if (!dir.exists()) {
                dir.mkdir();
                File readme = new File(dir, "REAMDE.md");
                readme.createNewFile();
                ByteStreams.copy(getResourceAsStream(directory + "/README.md"), new FileOutputStream(readme));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setupPurging(String type) {
        int days = getConfig().getInt("server." + type + "days");
        int purge = getConfig().getInt("server.purge", 10);
        if (purge > 0 && days > 0) {
            getProxy().getScheduler().schedule(this, new Runnable()
            {
                PurgeScheduler purge = new PurgeScheduler("stats", days);

                @Override
                public void run()
                {
                    executor.execute(purge);
                }
            }, purge, purge, TimeUnit.MINUTES);
        }
    }

    public static Configuration getConfig() {
        return config;
    }

    public static void reloadConfig(Plugin plugin) {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdir();
        ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        InputStream defaultStream = plugin.getResourceAsStream("config.yml");
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
                ByteStreams.copy(defaultStream, new FileOutputStream(configFile));
                plugin.getLogger().warning("A new configuration file has been created. Please edit config.yml and restart BungeeCord.");
                return;
            }
            config = provider.load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        defaultConfig = provider.load(new Scanner(defaultStream, "UTF-8").useDelimiter("\\A").next());
    }

    public static void log(ProxiedPlayer player, int type) {
        log(player, type, "");
    }

    public static void log(final ProxiedPlayer player, final int type, final String content)
    {
        executor.execute(() -> {
            try (Connection connection = manager.getStorage().getSQL().open();
                 Query query = connection.createQuery("INSERT INTO `" + getConfig().getString("database.prefix") + "log` (`time`, `type`, `uuid`, `username`," +
                         " `content`) " +
                         "VALUES(:time, :type, :uuid, :username, :content)"))
            {
                query.addParameter("time", System.currentTimeMillis() / 1000)
                        .addParameter("type", type)
                        .addParameter("uuid", getUUID(player))
                        .addParameter("username", player.getName())
                        .addParameter("content", content.length() > 100 ? content.substring(0, 99) : content)
                        .executeUpdate();
            }
        });
    }

    public static String getUUID(ProxiedPlayer p) {
        return p.getUniqueId().toString().replace("-", "");
    }

    public static LoginData getLogin(String user, String pass) throws Exception
    {
        if (user == null || pass == null) return null;

        return executor.submit(() -> {
            try (Connection connection = manager.getStorage().getSQL().open();
                 Query query = connection.createQuery("SELECT * FROM `" + BungeeWeb.getConfig().getString("database.prefix") + "users` WHERE `user`=:user"))
            {
                return query.addParameter("user", user)
                        .executeAndFetchFirst((ResultSetHandler<LoginData>) resultSet -> {
                            if (!resultSet.getString("pass").equals(BungeeWeb.encrypt(pass + resultSet.getString("salt"))))
                            {
                                return null;
                            }

                            return new LoginData(
                                    resultSet.getString("id"),
                                    resultSet.getString("user"),
                                    resultSet.getString("pass"),
                                    resultSet.getInt("group"));
                        });
            }
        }).get();
    }

    public static List getGroupPermissions(int group) {
        List<Object> permissions = new ArrayList<Object>();

        for (int i = group; i > 0; i--) {
            String key = "permissions.group" + i;
            permissions.addAll(config.getList(key, defaultConfig.getList(key)));
        }

        return permissions;
    }

    public static int getGroupPower(HttpServletRequest req) {
        int group = (Integer) req.getSession().getAttribute("group");
        if (group >= 3) group++;
        return group;
    }

    public static String encrypt(String pass) {
        return Password.MD5.digest(pass).split(":")[1];
    }

    public static String encrypt(String pass, String salt) {
        return encrypt(pass + salt);
    }

    public static String salt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return DatatypeConverter.printBase64Binary(salt).substring(0, 16);
    }

    public static boolean isNumber(String number) {
        int o;
        try {
            o = Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return false;
        }
        return o >= 0;
    }
}
