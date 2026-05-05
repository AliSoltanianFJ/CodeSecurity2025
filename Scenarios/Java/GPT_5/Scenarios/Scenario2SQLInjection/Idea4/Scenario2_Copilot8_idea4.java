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

public class Scenario2_Copilot8_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+\\-]{1,64}@[A-Za-z0-9.\\-]{1,255}\\.[A-Za-z]{2,63}$");
        private static final int MAX_EMAIL_LENGTH = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain; charset=utf-8");

            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing required parameter.");
                return;
            }

            email = email.trim();
            if (email.isEmpty() || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid parameter.");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOGGER.severe("Database credentials are not configured in environment variables.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal server error.");
                return;
            }

            String selectSql = "SELECT 1 FROM subscribers WHERE email = ? LIMIT 1";
            String updateSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ?";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                conn.setAutoCommit(false);
                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setString(1, email);
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().write("Not found.");
                            return;
                        }
                    }
                }

                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, email);
                    int updated = psUpdate.executeUpdate();
                    conn.commit();
                    if (updated > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Unsubscribed.");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("No change.");
                    }
                } catch (SQLException ex) {
                    conn.rollback();
                    throw ex;
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe.", ex);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal server error.");
            }
        }
    }
}
