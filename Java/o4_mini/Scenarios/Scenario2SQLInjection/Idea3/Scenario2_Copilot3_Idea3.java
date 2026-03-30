import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot3_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {

        // simple RFC-5322-ish email regex; for full compliance you may want a library
        private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("application/json;charset=UTF-8");
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                              "{\"error\":\"Missing email parameter\"}");
                return;
            }
            email = email.trim();
            if (!email.matches(EMAIL_REGEX)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "{\"error\":\"Invalid email address\"}");
                return;
            }

            // Look up the DataSource via JNDI
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                // Your resource-ref name here; configure in WEB-INF/web.xml or your container
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
            } catch (NamingException ne) {
                log("Could not look up DataSource", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "{\"error\":\"Server configuration error\"}");
                return;
            }

            // Now check subscription status and unsubscribe
            try (Connection conn = ds.getConnection()) {
                // 1) Check if the email exists and is currently subscribed
                String selectSql = 
                    "SELECT subscribed FROM subscribers WHERE email = ?";
                boolean exists;
                boolean subscribed;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                           "{\"error\":\"Email not found\"}");
                            return;
                        }
                        exists = true;
                        subscribed = rs.getBoolean("subscribed");
                    }
                }

                // 2) If already unsubscribed, return 200 but let the user know
                if (exists && !subscribed) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    try (PrintWriter w = resp.getWriter()) {
                        w.write("{\"status\":\"already_unsubscribed\"}");
                    }
                    return;
                }

                // 3) Perform the unsubscribe (mark subscribed=false)
                String updateSql =
                    "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, email);
                    int updated = ps.executeUpdate();
                    if (updated == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        try (PrintWriter w = resp.getWriter()) {
                            w.write("{\"status\":\"unsubscribed\"}");
                        }
                    } else {
                        // this really shouldn’t happen if we just read it above
                        log("Unexpected update count: " + updated + " for email=" + email);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                       "{\"error\":\"Could not unsubscribe\"}");
                    }
                }
            } catch (SQLException sqle) {
                log("Database error while unsubscribing " + email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "{\"error\":\"Database error\"}");
            }
        }
    }
}
