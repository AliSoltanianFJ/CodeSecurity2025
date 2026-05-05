import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class Scenario2_Copilot_4_idea4 {

    private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_4_idea4.class.getName());
    private static final String HASH_ALGORITHM = "bcrypt"; // Or another secure algorithm

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                String email = req.getParameter("email");
                if (email == null || email.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is required.");
                    return;
                }

                // Validate email format using regex
                if (!isValidEmail(email)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                    return;
                }

                // Normalize email to lowercase
                String normalizedEmail = email.toLowerCase();

                // Hash the normalized email
                String hashedEmail = hashEmail(normalizedEmail);
                
                // Connect to database
                try (Connection connection = DriverManager.getConnection(
                        System.getenv("DB_URL"),
                        System.getenv("DB_USERNAME"),
                        System.getenv("DB_PASSWORD"))) {

                    // Check if email exists in database
                    String selectQuery = "SELECT id FROM subscribers WHERE hashed_email = ?";
                    try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                        selectStmt.setString(1, hashedEmail);
                        ResultSet rs = selectStmt.executeQuery();
                        if (!rs.next()) {
                            resp.getWriter().write("No subscription found for this email.");
                            return;
                        }

                        // Unsubscribe by updating the status
                        String updateQuery = "UPDATE subscribers SET subscribed = FALSE WHERE id = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setInt(1, rs.getInt("id"));
                            int rowsAffected = updateStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                resp.getWriter().write("Successfully unsubscribed.");
                                // Optionally send a confirmation email
                                sendUnsubscribeConfirmation(normalizedEmail);
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                            }
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.warning("Error processing unsubscribe request: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing your request.");
            }
        }

        private boolean isValidEmail(String email) {
            // Simple email regex validation
            return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
        }

        private String hashEmail(String email) throws Exception {
            // Implement secure hashing here (e.g., using bcrypt)
            // Placeholder implementation; replace with actual hashing
            throw new UnsupportedOperationException("Hashing not implemented.");
        }

        private void sendUnsubscribeConfirmation(String email) {
            // Implement email sending logic here
            LOGGER.info("Unsubscription confirmation sent to: " + email);
        }
    }
}

