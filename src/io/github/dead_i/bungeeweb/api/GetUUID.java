package io.github.dead_i.bungeeweb.api;

import com.ayanix.panther.internal.sql2o.Connection;
import com.ayanix.panther.internal.sql2o.Query;
import com.ayanix.panther.internal.sql2o.ResultSetHandler;
import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class GetUUID extends APICommand {
    public GetUUID() {
        super("getuuid", null);
    }

    @Override
    public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String user = req.getParameter("username");
        if (user == null) {
            res.getWriter().print("{ \"error\": \"A username was not provided.\" }");
            return;
        }

        String rs = getByUsername(user, false);
        boolean matched = !rs.isEmpty();

        if (!matched) {
            rs = getByUsername(user, true);
            matched = !rs.isEmpty();
        }

        if (matched) {
            res.getWriter().print("{ \"uuid\": \"" + rs + "\" }");
        }else{
            res.getWriter().print("{ \"error\": \"No such username exists in the database.\" }");
        }
    }

    private String getByUsername(String search, boolean partial) throws SQLException {
        try(Connection connection = BungeeWeb.getManager().getStorage().getSQL().open();
            Query query = connection.createQuery("SELECT * FROM `" + BungeeWeb.getConfig().getString("database.prefix") + "log` WHERE `username` LIKE :conditions ORDER BY `id` DESC LIMIT 1")) {

            List<String> results = query.addParameter("conditions", partial ? "%" + search + "%" : search)
                    .executeAndFetch((ResultSetHandler<String>) resultSet -> resultSet.getString("uuid"));

            if(results.isEmpty()) {
                return "";
            }

            return results.get(0);
        }
    }
}
