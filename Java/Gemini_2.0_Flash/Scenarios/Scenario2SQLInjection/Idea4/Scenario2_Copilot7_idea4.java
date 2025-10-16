import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot7_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {

        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Database credentials and connection details
        private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
        private static final String DB_USER = "dbuser"; // Replace with your database username
        private static final String DB_PASSWORD = "dbpassword"; // Replace with your database password

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            // Validate email (basic check)
            if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            try {
                unsubscribeEmail(email, resp); // Pass the HttpServletResponse object
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during unsubscription.", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
            }
        }

        private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
            // Get database connection properties
            Properties dbProps = new Properties();
            dbProps.setProperty("user", DB_USER);
            dbProps.setProperty("password", DB_PASSWORD);
            dbProps.setProperty("useSSL", "false"); // Set to "true" in production

            // Use try-with-resources to ensure resources are closed properly
            try (Connection conn = DriverManager.getConnection(DB_URL, dbProps)) {
                // Prepare SQL statement to check if the email exists
                String checkEmailSQL = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkEmailSQL)) {
                    checkStmt.setString(1, email);

                    // Execute the query
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Email exists, proceed with unsubscription
                            String unsubscribeSQL = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement unsubscribeStmt = conn.prepareStatement(unsubscribeSQL)) {
                                unsubscribeStmt.setString(1, email);

                                // Execute the update
                                int rowsAffected = unsubscribeStmt.executeUpdate();

                                if (rowsAffected > 0) {
                                    resp.getWriter().println("Successfully unsubscribed " + email);
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                } else {
                                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the subscription list.");
                                }
                            }
                        } else {
                            // Email does not exist
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the subscription list.");
                        }
                    }
                }
            }
        }

        // Basic email validation
        private boolean isValidEmail(String email) {
            // A more comprehensive regex is recommended for production
            return email.contains("@");
        }
    }
}
