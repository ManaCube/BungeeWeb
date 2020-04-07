package io.github.dead_i.bungeeweb;

import com.google.common.io.ByteStreams;
import io.github.dead_i.bungeeweb.api.*;
import io.github.dead_i.bungeeweb.objects.LoginData;
import net.md_5.bungee.api.plugin.Plugin;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;

public class WebHandler extends AbstractHandler {
    private HashMap<String, APICommand> commands = new HashMap<String, APICommand>();
    private Plugin plugin;

    public WebHandler(Plugin plugin) {
        this.plugin = plugin;
        registerCommand(new ChangePassword());
        registerCommand(new CreateUser());
        registerCommand(new DeleteUser());
        registerCommand(new EditUser());
        registerCommand(new GetLang());
        registerCommand(new GetLogs());
        registerCommand(new GetServers());
        registerCommand(new GetSession());
        registerCommand(new GetStats());
        registerCommand(new GetTypes());
        registerCommand(new GetUsers());
        registerCommand(new GetUUID());
        registerCommand(new ListServers());
    }

    @Override
    public void handle(String target, Request baseReq, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setCharacterEncoding("utf8");

        if (target.equals("/")) target = "/index.html";
        String[] path = target.split("/");

        if (target.substring(target.length() - 1).equals("/")) {
            res.sendRedirect(target.substring(0, target.length() - 1));
            baseReq.setHandled(true);
        }else if (path.length > 2 && path[1].equalsIgnoreCase("api")) {
            if (commands.containsKey(path[2])) {
                try {
                    APICommand command = commands.get(path[2]);
                    if (command.hasPermission(req)) {
                        command.execute(plugin, req, res, path);
                    }else{
                        res.getWriter().print("{ \"error\": \"You do not have permission to perform this action.\" }");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("A MySQL database error occurred.");
                    e.printStackTrace();
                }
                baseReq.setHandled(true);
            }
        }else if (path.length > 1 && path[1].equalsIgnoreCase("login")) {
            try
            {
                LoginData loginData = BungeeWeb.getLogin(req.getParameter("user"), req.getParameter("pass"));

                if (req.getMethod().equals("POST") && loginData != null)
                {
                    req.getSession().setAttribute("id", loginData.getId());
                    req.getSession().setAttribute("user", loginData.getUsername());
                    req.getSession().setAttribute("group", loginData.getGroup());

                    res.getWriter().print("{ \"status\": 1 }");
                } else
                {
                    res.getWriter().print("{ \"status\": 0 }");
                }

                baseReq.setHandled(true);
            } catch (Exception e)
            {
                plugin.getLogger().warning("A database error occurred.");
                e.printStackTrace();
            }
        }else if (path.length > 1 && path[1].equalsIgnoreCase("logout")) {
            req.getSession().invalidate();
            res.sendRedirect("/");
            baseReq.setHandled(true);
        }else if (target.equalsIgnoreCase("/css/theme.css")) {
            String name = BungeeWeb.getConfig().getString("server.theme");
            if (name.isEmpty()) name = "dark";
            InputStream resource = plugin.getResourceAsStream("themes/" + name + ".css");
            if (resource == null) {
                File file = new File(plugin.getDataFolder(), "themes/" + name + ".css");
                if (file.exists()) {
                    ByteStreams.copy(new FileInputStream(file), res.getOutputStream());
                }
            }else{
                ByteStreams.copy(resource, res.getOutputStream());
            }
            baseReq.setHandled(true);
        }else{
            String file = "web" + target;
            InputStream stream = plugin.getResourceAsStream(file);
            if (stream == null && path.length == 2) {
                file = "web/index.html";
                stream = plugin.getResourceAsStream(file);
            }
            if (stream != null) {
                baseReq.setHandled(true);
                res.setContentType(getContentType(file));
                ByteStreams.copy(stream, res.getOutputStream());
            }
        }
    }

    public void registerCommand(APICommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    public String getContentType(String filename) {
        MimetypesFileTypeMap map = new MimetypesFileTypeMap();
        map.addMimeTypes("text/html html htm");
        map.addMimeTypes("text/javascript js json");
        map.addMimeTypes("text/css css");
        map.addMimeTypes("image/jpeg jpg jpeg");
        map.addMimeTypes("image/gif gif");
        map.addMimeTypes("image/png png");
        return map.getContentType(filename.toLowerCase());
    }
}
