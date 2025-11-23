// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot1_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic, conservative email pattern: no exotic Unicode, enforces reasonable length
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,189}\\.[A-Za-z]{2,}$"
        );
        private static final int MAX_EMAIL_LENGTH = 254;

        // Use environment variables for DB credentials to avoid hardcoding secrets.
        private static final String DB_URL = System.getenv("DB_URL");       // e.g. "jdbc:postgresql://dbhost:5432/dbname"
        private static final String DB_USER = System.getenv("DB_USER");
        private static final String DB_PASS = System.getenv("DB_PASS");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Security headers
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            // Do not enable HSTS here unless you control TLS termination; set via server config when appropriate.

            String email = req.getParameter("email");
            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.println("Missing required parameter.");
                }
                return;
            }

            email = email.trim();
            if (email.length() == 0 || email.length() > MAX_EMAIL_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.println("Invalid email parameter.");
                }
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.println("Invalid email format.");
                }
                return;
            }

            // Ensure DB configuration is present
            if (DB_URL == null || DB_USER == null || DB_PASS == null) {
                logger.log(Level.SEVERE, "Database configuration missing (DB_URL/DB_USER/DB_PASS not set).");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.println("Service unavailable.");
                }
                return;
            }

            // Use parameterized query to prevent SQL injection. Use try-with-resources to avoid resource leaks.
            // Assumes a table 'subscribers' with a boolean 'subscribed' column or similar; modify to fit schema.
            String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {

                ps.setString(1, email);
                int affected = ps.executeUpdate();

                try (PrintWriter pw = resp.getWriter()) {
                    if (affected > 0) {
                        // Do NOT echo back the raw email to avoid reflected XSS or information leakage.
                        pw.println("If the address existed, it has been unsubscribed.");
                        resp.setStatus(HttpServletResponse.SC_OK);
                    } else {
                        // Same response to avoid user enumeration
                        pw.println("If the address existed, it has been unsubscribed.");
                        resp.setStatus(HttpServletResponse.SC_OK);
                    }
                }

            } catch (SQLException e) {
                // Log details for operators, but return a generic message to the client.
                logger.log(Level.SEVERE, "Database error while unsubscribing (email hidden).", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.println("Service unavailable.");
                }
            } catch (Exception e) {
                // Catch-all to avoid propagating unexpected exceptions
                logger.log(Level.SEVERE, "Unexpected error while processing unsubscribe request.", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter pw = resp.getWriter()) {
                    pw.println("Service unavailable.");
                }
            }
        }
    }
}
// ...existing code...
