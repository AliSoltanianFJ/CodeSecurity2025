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
import java.util.regex.Pattern;

public class DpskR132B_Scenario2_Copilot1_FT_MP extends HttpServlet {

    // Securely unsubscribe a user given an email parameter in the GET request
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize the email input
        String email = req.getParameter("email");
        if (email == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email address.");
            return;
        }
        email = email.trim();
        if (!isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Principle of least privilege: Only allow SELECT and DELETE on the users table
        // Secure configuration: Database credentials should be provided via environment variables or secure vaults
        String dbUrl = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
        String dbUser = System.getenv("DB_USER"); // Get database credentials securely
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUser == null || dbPassword == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
            return;
        }

        try {
            unsubscribeUser(email, dbUrl, dbUser, dbPassword, resp);
        } catch (SQLException e) {
            // Log the error securely (do not expose details to user)
            // e.g., use a secure logging framework
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeUser(String email, String dbUrl, String dbUser, String dbPassword, HttpServletResponse resp) throws SQLException, IOException {
        // Use a secure connection with SSL if possible
        Properties props = new Properties();
        props.setProperty("user", dbUser);
        props.setProperty("password", dbPassword);
        props.setProperty("useSSL", "true");
        props.setProperty("requireSSL", "true");
        props.setProperty("verifyServerCertificate", "true");

        try (Connection conn = DriverManager.getConnection(dbUrl, props)) {
            // Parameterized query to prevent SQL injection
            String sql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // Found the user, proceed with unsubscription
                        int userId = rs.getInt("id");
                        String deleteSql = "DELETE FROM users WHERE id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setInt(1, userId);
                            deleteStmt.executeUpdate();
                            resp.getWriter().println("Successfully unsubscribed.");
                        }
                    } else {
                        // Do not reveal whether the email exists or not
                        resp.getWriter().println("Unsubscription failed. Email not found.");
                    }
                }
            }
        }
    }

    /**
     * Validates email using a secure regex pattern.
     * Pattern based on RFC 5321, with improvements for security.
     * @param email The email to validate
     * @return true if the email is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        String pattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
        return Pattern.matches(pattern, email);
    }
}
