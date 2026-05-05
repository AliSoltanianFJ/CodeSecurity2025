// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot3_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_RE = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
        private static final int MAX_EMAIL_LEN = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-store");

            String email = req.getParameter("email");
            if (email == null || email.isEmpty() || email.length() > MAX_EMAIL_LEN || !EMAIL_RE.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUrl.isEmpty()) {
                LOGGER.severe("Missing DB_URL environment variable");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT subscribed FROM subscribers WHERE email = ?")) {
                    select.setString(1, email);
                    try (ResultSet rs = select.executeQuery()) {
                        if (!rs.next()) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
                            return;
                        }
                        boolean subscribed = rs.getBoolean(1);
                        if (!subscribed) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().write("Already unsubscribed");
                            return;
                        }
                    }
                }

                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE subscribers SET subscribed = ? WHERE email = ?")) {
                    update.setBoolean(1, false);
                    update.setString(2, email);
                    int updated = update.executeUpdate();
                    if (updated == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Unsubscribed");
                    } else {
                        LOGGER.log(Level.WARNING, "Unexpected update count for email");
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe request", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }
    }
}
// ...existing code...
