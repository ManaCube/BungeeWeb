package io.github.dead_i.bungeeweb.api;

import com.google.gson.Gson;
import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

public class GetSession extends APICommand {
    private Gson gson = new Gson();

    public GetSession() {
        super("getsession", true);
    }

    @Override
    public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException
    {
        HashMap<String, Object> out = new HashMap<String, Object>();

        Integer group = (Integer) req.getSession().getAttribute("group");
        if (group == null) {
            out.put("group", 0);
        }else{
            out.put("id", req.getSession().getAttribute("id"));
            out.put("user", req.getSession().getAttribute("user"));
            out.put("group", group);
            out.put("updatetime", BungeeWeb.getConfig().getInt("server.updatetime", 10));
            out.put("permissions", BungeeWeb.getGroupPermissions(group));
        }
        out.put("autosearch", BungeeWeb.getConfig().getBoolean("server.autosearch"));
        out.put("transitions", !BungeeWeb.getConfig().getBoolean("server.disabletransitions"));

        res.getWriter().print(gson.toJson(out));
    }
}
