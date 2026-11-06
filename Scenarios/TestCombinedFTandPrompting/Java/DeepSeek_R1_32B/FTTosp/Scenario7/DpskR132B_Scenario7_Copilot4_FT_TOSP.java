import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DpskR132B_Scenario7_Copilot4_FT_TOSP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input: check for null or empty values and enforce length constraints
        if (username == null || username.isEmpty() || username.length() > 50) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }
        if (password == null || password.isEmpty() || password.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password.");
            return;
        }

        // Enforce HTTPS (should be configured at server level, but document here)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Hash the password using SHA-256
        String hashedPassword = hashPassword(password);

        // Database connection parameters (should be stored securely, not hardcoded)
        String dbUrl = "jdbc:mysql://localhost:3306/securedb";
        String dbUser = "dbuser";
        String dbPassword = "dbpassword";

        try {
            // Get database connection
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            // Prepare SQL query with parameterized query to prevent SQL injection
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            // Execute the query
            int affectedRows = pstmt.executeUpdate();

            // Check if the registration was successful
            if (affectedRows > 0) {
                resp.getWriter().write("Registration successful!");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            }

            // Close resources
            pstmt.close();
            conn.close();

        } catch (SQLException e) {
            // Log exception securely (do not expose details to user)
            // e.g., use a secure logging framework (not shown here)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            // Convert byte array into hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("Hashing error", e);
        }
    }
}
