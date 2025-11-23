import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot9_idea1 {

    // ...existing code...

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                Pattern.CASE_INSENSITIVE
        );

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            try (PrintWriter out = resp.getWriter()) {
                if (email == null || email.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Missing 'email' parameter.");
                    return;
                }
                email = email.trim();

                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Invalid email format.");
                    return;
                }

                // Read DB configuration from environment variables (set these in your environment)
                final String DB_URL = System.getenv("DB_URL");        // e.g. "jdbc:postgresql://localhost:5432/mydb"
                final String DB_USER = System.getenv("DB_USER");
                final String DB_PASSWORD = System.getenv("DB_PASSWORD");

                if (DB_URL == null || DB_URL.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Database not configured.");
                    return;
                }

                // Use prepared statements and transactions to avoid races and SQL injection
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement selectStmt = conn.prepareStatement(
                            "SELECT subscribed FROM users WHERE email = ?")) {
                        selectStmt.setString(1, email);
                        try (ResultSet rs = selectStmt.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                out.println("Email not found.");
                                return;
                            }
                            boolean subscribed = rs.getBoolean("subscribed");
                            if (!subscribed) {
                                conn.rollback();
                                resp.setStatus(HttpServletResponse.SC_OK);
                                out.println("Already unsubscribed.");
                                return;
                            }
                        }
                    }

                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE users SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE")) {
                        updateStmt.setString(1, email);
                        int updated = updateStmt.executeUpdate();
                        if (updated == 1) {
                            conn.commit();
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.println("Unsubscribed successfully.");
                        } else {
                            conn.rollback();
                            LOGGER.log(Level.WARNING, "Unexpected update count ({0}) for email {1}", new Object[]{updated, email});
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Unable to unsubscribe at this time.");
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Database error while unsubscribing: " + email, e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error. Please try again later.");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Unexpected error while unsubscribing: " + email, e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error. Please try again later.");
                }
            }
        }
    }

    // ...existing code...
}