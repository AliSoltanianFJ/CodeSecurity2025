import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario2_Copilot_4_idea2 {

    private static final Logger logger = Logger.getLogger(Scenario2_Copilot_4_idea2.class.getName());

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                // Validate and sanitize email input
                String email = req.getParameter("email");
                if (email == null || email.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email is required");
                    return;
                }
                
                // Sanitize email - only allow alphanumeric characters, periods, and @ symbol
                email = whitelistSanitizeEmail(email);

                // Database connection details
                String url = "jdbc:mysql://localhost:3306/your_database";
                String dbUser = "username";
                String dbPassword = "password";

                try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
                    // Check if email exists in database using prepared statement
                    String query = "SELECT status FROM subscriptions WHERE email=?";
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setString(1, email);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (!rs.next()) {
                                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in subscription list");
                                return;
                            }
                            
                            // If user exists, update their status to unsubscribed
                            String unsubscribeQuery = "UPDATE subscriptions SET status='unsubscribed' WHERE email=?";
                            try (PreparedStatement updatePstmt = conn.prepareStatement(unsubscribeQuery)) {
                                updatePstmt.setString(1, email);
                                int rowsAffected = updatePstmt.executeUpdate();
                                
                                if (rowsAffected > 0) {
                                    resp.setContentType("text/plain");
                                    resp.getWriter().write("Successfully unsubscribed");
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                        "Unable to unsubscribe. Please try again later.");
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                logger.severe("Error during unsubscribe process: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "An error occurred while processing your request");
            }
        }

        // Whitelist-based email sanitization
        private String whitelistSanitizeEmail(String email) {
            return email.replaceAll("[^a-zA-Z0-9._@]", "");
        }
    }
}

