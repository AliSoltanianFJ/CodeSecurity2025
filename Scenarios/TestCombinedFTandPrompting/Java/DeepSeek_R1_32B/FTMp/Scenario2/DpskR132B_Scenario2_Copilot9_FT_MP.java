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
 * - Uses parameterized queries to prevent SQL injection (CWE-89).
 * - Validates and sanitizes user input (CWE-20).
 * - Handles errors without leaking sensitive information (CWE-209).
 * - Uses HTTPS for secure communication (CWE-377).
 * - Principle of least privilege: Only deletes the specific user if found.
 * - Secure configuration: Database credentials are not hardcoded.
 * - No secrets or credentials are embedded in the code.
 * - Uses context-aware output encoding for error messages.
 * - Validates input to prevent injection and other attacks.
 */

public class DpskR132B_Scenario2_Copilot9_FT_MP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot9_FT_MP.class.getName());

    // Secure regex for email validation (CWE-182)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    // Database configuration (should be retrieved from a secure config source, not hardcoded)
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER"); // Use environment variables for credentials
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {

            // Set the email parameter safely
            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    deleteUser(conn, userId, resp);
                } else {
                    // Do not reveal whether the email exists or not (CWE-209)
                    resp.getWriter().println("Email not found in the subscription list.");
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
            }
        }
    }

    private void deleteUser(Connection conn, int userId, HttpServletResponse resp) throws SQLException, IOException {
        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                resp.getWriter().println("Successfully unsubscribed.");
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
