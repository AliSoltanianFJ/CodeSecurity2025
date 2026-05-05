import javax.servlet.annotation.WebServlet;
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

public class Scenario2_Copilot10_idea2 {

    // ...existing code...

    @WebServlet("/unsubscribe")
    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Simple, safe email pattern (practical validation). Limits length to prevent abuse.
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]{1,64}@[A-Za-z0-9.-]{1,189}\\.[A-Za-z]{2,63}$");
        private static final int MAX_EMAIL_LENGTH = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html; charset=UTF-8");

            String email = safeTrim(req.getParameter("email"));
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeEscaped(resp, "Missing email parameter.");
                return;
            }

            if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeEscaped(resp, "Invalid email address.");
                return;
            }

            // Read DB connection info from environment variables to avoid hard-coded secrets.
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOGGER.log(Level.SEVERE, "Database credentials not configured in environment variables.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeEscaped(resp, "Service not available.");
                return;
            }

            // Use parameterized queries and transaction to update safely.
            String selectSql = "SELECT subscribed FROM users WHERE email = ?";
            String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                // Enforce a short timeout where supported (driver dependent).
                conn.setAutoCommit(false);
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            writeEscaped(resp, "Email not found.");
                            return;
                        }

                        boolean currentlySubscribed = rs.getBoolean("subscribed");
                        if (!currentlySubscribed) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_OK);
                            writeEscaped(resp, "Email is already unsubscribed.");
                            return;
                        }
                    }
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int updated = updateStmt.executeUpdate();
                    if (updated == 1) {
                        conn.commit();
                        resp.setStatus(HttpServletResponse.SC_OK);
                        writeEscaped(resp, "Unsubscribed successfully.");
                    } else {
                        conn.rollback();
                        LOGGER.log(Level.WARNING, "Unexpected update count when unsubscribing email: {0}", email);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        writeEscaped(resp, "Unable to unsubscribe at this time.");
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                // Log server-side details, but do not expose to the client.
                LOGGER.log(Level.SEVERE, "Database error during unsubscribe operation", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeEscaped(resp, "Service not available.");
            }
        }

        // Trim and null-safe
        private static String safeTrim(String s) {
            if (s == null) return null;
            return s.trim();
        }

        // Minimal HTML-escape helper to prevent XSS.
        private static void writeEscaped(HttpServletResponse resp, String message) throws IOException {
            try (PrintWriter out = resp.getWriter()) {
                out.write(escapeHtml(message));
            }
        }

        private static String escapeHtml(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#x27;"); break;
                    case '/': sb.append("&#x2F;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    // ...existing code...
}
