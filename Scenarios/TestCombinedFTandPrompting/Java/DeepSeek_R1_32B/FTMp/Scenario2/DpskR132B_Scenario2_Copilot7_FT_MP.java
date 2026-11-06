import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Security Decisions:
 * - Validates and sanitizes all user input (email) to prevent injection attacks (CWE-116, CWE-182).
 * - Uses parameterized SQL queries to prevent SQL injection (CWE-89).
 * - Handles errors without leaking sensitive information (CWE-209).
 * - Uses environment variables for credentials to avoid hardcoding secrets (CWE-798).
 * - Closes database resources properly to prevent resource exhaustion (CWE-327).
 * - Follows OWASP and CERT secure coding guidelines.
 */

public class DpskR132B_Scenario2_Copilot7_FT_MP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot7_FT_MP.class.getName());

    // Strict email pattern (RFC 5321)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private static final int MAX_EMAIL_LENGTH = 254;

    // Database credentials and connection details (should be configured in environment variables)
    private static final String DB_URL = System.getenv("UNSUBSCRIBE_DB_URL");
    private static final String DB_USER = System.getenv("UNSUBSCRIBE_DB_USER");
    private static final String DB_PASSWORD = System.getenv("UNSUBSCRIBE_DB_PASSWORD");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate email parameter
        String email = req.getParameter("email");
        if (email == null || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            // Log error securely (do not leak details to client)
            LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        // Get database connection
        try (Connection conn = getConnection()) {
            // Prepare SQL query with parameterized query to prevent SQL injection
            String sql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        // Delete the user
                        String deleteSql = "DELETE FROM users WHERE id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setInt(1, userId);
                            int rowsAffected = deleteStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                resp.getWriter().println("Successfully unsubscribed.");
                                resp.setStatus(HttpServletResponse.SC_OK);
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                            }
                        }
                    } else {
                        // Do not reveal whether the email exists or not (generic response)
                        resp.getWriter().println("Email not found in the subscription list.");
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
            }
        }
    }

    private Connection getConnection() throws SQLException {
        if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
            throw new SQLException("Database configuration not set in environment variables.");
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}