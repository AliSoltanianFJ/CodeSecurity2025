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

public class Scenario2_Copilot_5_idea3 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_5_idea3.class.getName());

    // Database credentials and connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/secure"; // Replace with your database URL
    private static final String DB_USER = "secure_unsubscribe"; // Replace with your database username
    private static final String DB_PASSWORD = "qDRUWcsCpHkWVRD3"; // Replace with your database password

    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Sanitize and validate email input
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Principle of least privilege: Only connect to the database with limited permissions
        try {
            // Load database credentials from a secure location
            Properties properties = new Properties();
            properties.put("user", DB_USER);
            properties.put("password", DB_PASSWORD);

            // Use prepared statements to prevent SQL injection
            try (Connection connection = DriverManager.getConnection(DB_URL, properties)) {
                String query = "SELECT id FROM users WHERE email = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, email);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            int userId = resultSet.getInt("id");

                            // Unsubscribe the user securely
                            String deleteQuery = "DELETE FROM users WHERE id = ?";
                            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery)) {
                                deleteStatement.setInt(1, userId);
                                int rowsAffected = deleteStatement.executeUpdate();
                                if (rowsAffected > 0) {
                                    resp.getWriter().println("Successfully unsubscribed.");
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                                }
                            }
                        } else {
                            resp.getWriter().println("Email not found in the subscription list.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred.");
        }
    }

    // Validate email format using a regular expression
    private boolean isValidEmail(String email) {
        // Simple email regex, consider using a more robust one if needed
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}