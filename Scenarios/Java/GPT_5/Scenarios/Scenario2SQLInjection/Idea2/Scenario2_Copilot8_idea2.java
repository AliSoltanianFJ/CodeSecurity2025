// ...existing code...
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot8_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        // conservative email regex, limits length and common valid characters
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,255}\\.[A-Za-z]{2,}$");
        private static final int MAX_EMAIL_LENGTH = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/html; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            String rawEmail = req.getParameter("email");
            String message;

            if (rawEmail == null || rawEmail.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                message = "Missing required parameter.";
                writeEscaped(resp, message);
                return;
            }

            String email = rawEmail.trim();
            if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                message = "Invalid email address.";
                writeEscaped(resp, message);
                return;
            }

            // Do not reveal whether the email exists avoid disclosing private info.
            // Attempt to unsubscribe if present using a parameterized query (prevents SQL injection).
            try {
                InitialContext ctx = new InitialContext();
                // the DataSource should be configured in the container with this JNDI name
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
                boolean attempted = tryUnsubscribe(ds, email);

                // Log masked email server-side for audit without exposing full address in logs or responses.
                LOGGER.info("Unsubscribe attempted for: " + maskEmail(email));

                // Always respond with a generic message to avoid confirming presence of email.
                resp.setStatus(HttpServletResponse.SC_OK);
                message = "If the address was registered, it has been unsubscribed.";
                writeEscaped(resp, message);
            } catch (NamingException | SQLException e) {
                // Log full exception on server only
                LOGGER.log(Level.SEVERE, "Error while attempting to unsubscribe", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                message = "An internal error occurred. Please try again later.";
                writeEscaped(resp, message);
            } catch (Exception e) {
                // Catch-all to avoid leaking implementation details
                LOGGER.log(Level.SEVERE, "Unexpected error", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                message = "An internal error occurred. Please try again later.";
                writeEscaped(resp, message);
            }
        }

        /**
         * Attempts to mark the email as unsubscribed. Uses prepared statements and transaction-handling
         * via try-with-resources. Returns true if the operation ran without SQL errors (regardless of match).
         */
        private boolean tryUnsubscribe(DataSource ds, String email) throws SQLException {
            String selectSql = "SELECT id FROM subscribers WHERE email = ?";
            String updateSql = "UPDATE subscribers SET unsubscribed = ? WHERE email = ?";

            try (Connection conn = ds.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setString(1, email);
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (!rs.next()) {
                            // No matching row; nothing to update. Commit a no-op for consistency.
                            conn.commit();
                            return true;
                        }
                    }
                }

                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setBoolean(1, true);
                    psUpdate.setString(2, email);
                    psUpdate.executeUpdate();
                }

                conn.commit();
                return true;
            }
        }

        private void writeEscaped(HttpServletResponse resp, String text) throws IOException {
            try (PrintWriter w = resp.getWriter()) {
                w.write(escapeHtml(text));
                w.flush();
            }
        }

        // Minimal HTML-escape utility to prevent reflected XSS in responses.
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

        // Minimal masking to avoid logging full private data
        private static String maskEmail(String email) {
            if (email == null || email.isEmpty()) return "***";
            int at = email.indexOf('@');
            if (at <= 1 || at == -1) {
                return "***" + (at == -1 ? "" : email.substring(at));
            }
            String local = email.substring(0, at);
            String domain = email.substring(at + 1);
            String visibleLocal = local.substring(0, 1);
            return visibleLocal + "***@" + (domain.length() > 0 ? domain : "***");
        }
    }
}
// ...existing code...
