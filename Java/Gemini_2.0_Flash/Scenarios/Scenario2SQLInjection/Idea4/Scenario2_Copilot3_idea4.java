import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;

public class Scenario2_Copilot3_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {

        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Database credentials - NEVER hardcode in real applications.  Use environment variables or a secure configuration.
        private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
        private static final String DB_USER = "dbuser"; // Replace with your database username
        private static final String DB_PASSWORD = "dbpassword"; // Replace with your database password

        // Email validation pattern
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

        /**
         * Validates the email format.
         *
         * @param email The email to validate.
         * @return true if the email is valid, false otherwise.
         */
        private boolean isValidEmail(String email) {
            if (email == null) {
                return false;
            }
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            return matcher.matches();
        }

        /**
         * Unsubscribes the email from the database.
         *
         * @param email The email to unsubscribe.
         * @return true if the email was successfully unsubscribed, false otherwise.
         */
        private boolean unsubscribeEmail(String email) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Prepare SQL statement to prevent SQL injection
                String sql = "UPDATE subscribers SET is_subscribed = FALSE WHERE email = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, email);

                    int affectedRows = pstmt.executeUpdate();
                    // Check if any rows were affected to determine if the unsubscribe was successful
                    return affectedRows > 0;
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error during unsubscribe: " + e.getMessage(), e);
                return false;
            }
        }

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            // Validate email format
            if (email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing.");
                return;
            }

            if (!isValidEmail(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // Unsubscribe the email
            boolean unsubscribed = unsubscribeEmail(email);

            if (unsubscribed) {
                resp.getWriter().println("Successfully unsubscribed " + email);
            } else {
                resp.getWriter().println("Email " + email + " not found or already unsubscribed.");
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            doGet(req, resp); // Treat POST requests the same as GET requests
        }
    }
}
