import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot2_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
        private static final int MAX_EMAIL_LENGTH = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=utf-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing required parameter.");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid email.");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Service unavailable.");
                LOGGER.log(Level.SEVERE, "Missing DB connection environment variables.");
                return;
            }

            String selectSql = "SELECT 1 FROM subscribers WHERE email = ? AND subscribed = 1";
            String updateSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ? AND subscribed = 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                conn.setAutoCommit(false);
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().write("Email not found.");
                            return;
                        }
                    }
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int updated = updateStmt.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        resp.setStatus(HttpServletResponse.SC_CONFLICT);
                        resp.getWriter().write("Unable to unsubscribe.");
                        return;
                    }
                }

                conn.commit();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Unsubscribed.");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe request.", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Service unavailable.");
            }
        }
    }
}
