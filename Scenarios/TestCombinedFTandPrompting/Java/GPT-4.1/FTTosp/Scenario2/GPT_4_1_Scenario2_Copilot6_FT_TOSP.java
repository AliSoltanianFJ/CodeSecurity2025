import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
// Secure imports
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario2_Copilot6_FT_TOSP extends HttpServlet {
    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        // Validate email input
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
            return;
        }

        // Principle of least privilege: do not hardcode credentials, assume a secure connection provider
        try (Connection conn = SecureDbConnectionProvider.getConnection()) {
            // Use prepared statements to prevent SQL injection
            String checkQuery = "SELECT id FROM subscribers WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int subscriberId = rs.getInt("id");
                        // Unsubscribe logic
                        String deleteQuery = "DELETE FROM subscribers WHERE id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                            deleteStmt.setInt(1, subscriberId);
                            int rowsAffected = deleteStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                resp.getWriter().write("Unsubscribed successfully.");
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
                            }
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                    }
                }
            }
        } catch (SQLException e) {
            // Log error securely, do not leak details to the user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred. Please try again later.");
        }
    }

    // Email validation using regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
