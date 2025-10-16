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

public class Scenario2_Copilot4_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Database configuration - NEVER hardcode credentials.  Use environment variables or a secure configuration file.
        private static final String DB_URL = System.getenv("DB_URL"); // Example: "jdbc:postgresql://localhost:5432/mydb"
        private static final String DB_USER = System.getenv("DB_USER");
        private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            // Validate email - VERY IMPORTANT to prevent SQL injection and other attacks
            if (email == null || email.isEmpty() || !isValidEmail(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            try {
                unsubscribeEmail(email, resp); // Pass the response object
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
            }
        }

        private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
            // All the database logic
            String sql = "UPDATE users SET subscribed = FALSE WHERE email = ?"; // Using prepared statement to prevent SQL injection

            // Use try-with-resources to ensure resources are closed properly
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, email); // Set the email parameter

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    resp.getWriter().println("Successfully unsubscribed.");
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.getWriter().println("Email not found in subscription list.");
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL Exception", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
                throw e; // Re-throw the exception to be caught in the doGet method
            }
        }

        private Connection getConnection() throws SQLException {
            // Using Properties to pass credentials
             Properties props = new Properties();
             props.setProperty("user", DB_USER);
             props.setProperty("password", DB_PASSWORD);
             // Disable SSL verification if not needed, but understand the risks.  NEVER do this in production without a very good reason.
             props.setProperty("ssl", "false");
             props.setProperty("sslmode", "disable");

            try {
                Class.forName("org.postgresql.Driver"); // Ensure the driver class is loaded
                return DriverManager.getConnection(DB_URL, props);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "PostgreSQL Driver not found", e);
                throw new SQLException("PostgreSQL Driver not found", e); // Wrap in SQLException
            }
        }

        private boolean isValidEmail(String email) {
            // Email validation regex (OWASP recommended)
            String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
            return email.matches(regex);
        }
    }
}
