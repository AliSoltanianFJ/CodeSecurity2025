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
import java.util.regex.Pattern;

public class Scenario2_Copilot_7_idea3 extends HttpServlet {
    // Database credentials and connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
    private static final String DB_USER = "."; // Replace with your database username
    private static final String DB_PASSWORD = "."; // Replace with your database password

    // Basic (practical) email pattern for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", 
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            // Log the exception server-side
            System.out.println("Database error during unsubscription: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {

            // Set the email parameter
            pstmt.setString(1, email);

            // Try to find the user
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");

                    // Now perform the deletion using another prepared statement
                    try (PreparedStatement deleteStmt = conn.prepareStatement(
                            "DELETE FROM users WHERE id = ?")) {

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
                    // Email not found
                    resp.getWriter().println("Email not found in the subscription list.");
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
            }
        }
    }

    private Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);
        // Disable SSL if not properly configured
        props.setProperty("useSSL", "false");
        props.setProperty("verifyServerCertificate", "false");
        props.setProperty("requireSSL", "false");
        // Set connection timeout (e.g., 3 seconds)
        props.setProperty("connectTimeout", "3000");

        return DriverManager.getConnection(DB_URL, props);
    }
}