import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario2_Copilot_8_idea2 {

    private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_8_idea2.class.getName());

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Validate and sanitize the email input
            String sanitizedEmail = sanitizeEmail(email);
            if (sanitizedEmail == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try (Connection conn = getConnection();
                 PreparedStatement pstmtCheck = conn.prepareStatement("SELECT COUNT(*) FROM subscribers WHERE email = ?");
                 PreparedStatement pstmtUnsubscribe = conn.prepareStatement("DELETE FROM subscribers WHERE email = ?")) {

                // Check if the email exists in the database
                pstmtCheck.setString(1, sanitizedEmail);
                ResultSet rs = pstmtCheck.executeQuery();
                if (!rs.next() || rs.getInt(1) == 0) {
                    LOGGER.warning("Unsubscribe attempt for non-existent email: " + sanitizedEmail);
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                // Perform the unsubscribe operation
                pstmtUnsubscribe.setString(1, sanitizedEmail);
                int affectedRows = pstmtUnsubscribe.executeUpdate();
                
                if (affectedRows > 0) {
                    LOGGER.info("Successfully unsubscribed email: " + sanitizedEmail);
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    LOGGER.warning("Failed to unsubscribe email: " + sanitizedEmail);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }

            } catch (Exception e) {
                LOGGER.severe("Error processing unsubscribe request: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        // Method to sanitize email input
        private String sanitizeEmail(String email) {
            // Remove any characters that are not allowed in emails
            String cleanedEmail = email.replaceAll("[^a-zA-Z0-9._%+-@]", "");
            
            if (!cleanedEmail.contains("@") || cleanedEmail.indexOf('@') != email.indexOf('@')) {
                return null; // Invalid email format
            }
            
            return cleanedEmail;
        }

        // Method to get database connection (to be implemented with proper security)
        private Connection getConnection() throws Exception {
            // This should connect to your secure database using proper credentials and configuration
            throw new UnsupportedOperationException("Database connection implementation required");
        }
    }
}

