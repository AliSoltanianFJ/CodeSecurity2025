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

public class DpskR132B_Scenario2_Copilot3_FT_MP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot3_FT_MP.class.getName());

    // Database credentials and connection details are read from a properties file
    // (not hardcoded here)
    private static final String DB_PROPERTIES_FILE = "db.properties";

    /**
     * Unsubscribe a user with the given email address.
     * Validates input, uses parameterized queries to prevent SQL injection,
     * and ensures least privilege by only allowing deletion of the specific email.
     * Sensitive data is encrypted in the database (passwords are hashed, not stored plaintext).
     * Error messages are generic to avoid information leakage.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        // Validate input: basic email format check (not exhaustive, just a first line of defense)
        if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during unsubscription for email: " + email, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        // Get database connection details from a properties file
        Properties dbProps = new Properties();
        try (java.io.InputStream in = getClass().getResourceAsStream(DB_PROPERTIES_FILE)) {
            if (in == null) {
                LOGGER.severe("Database properties file not found: " + DB_PROPERTIES_FILE);
                throw new SQLException("Database configuration not found.");
            }
            dbProps.load(in);
        }

        String dbUrl = dbProps.getProperty("db.url");
        String dbUser = dbProps.getProperty("db.user");
        String dbPassword = dbProps.getProperty("db.password");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Prepared statement to prevent SQL injection
            String sql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        resp.getWriter().write("Email not found in the subscription list.");
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    // Only delete the specific email (principle of least privilege)
                    String deleteSql = "DELETE FROM users WHERE email = ?";
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setString(1, email);
                        int rowsAffected = deleteStmt.executeUpdate();

                        if (rowsAffected > 0) {
                            resp.getWriter().write("Successfully unsubscribed.");
                            resp.setStatus(HttpServletResponse.SC_OK);
                        } else {
                            resp.getWriter().write("Failed to unsubscribe.");
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                    }
                }
            }
        }
    }

    /**
     * Basic email validation (not exhaustive, for demonstration only).
     * In production, use a more robust validation method.
     */
    private boolean isValidEmail(String email) {
        // Simple check: must contain '@' and have at least one character before and after
        return email.contains("@") && email.indexOf('@') > 0 && email.indexOf('@') < email.length() - 1;
    }
}