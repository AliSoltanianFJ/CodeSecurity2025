import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DpskR132B_Scenario7_Copilot4_FT_MP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input: ensure username and password are not null or empty
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            resp.getWriter().write("Error: Username and password are required.");
            return;
        }

        // Principle of least privilege: only allow alphanumeric usernames up to 32 characters
        if (!username.matches("^[a-zA-Z0-9]{1,32}$")) {
            resp.getWriter().write("Error: Username must be 1-32 alphanumeric characters.");
            return;
        }

        // Securely hash the password using SHA-256
        String hashedPassword = hashPassword(password);

        // Use parameterized query to prevent SQL injection
        try {
            // Get database connection (credentials should be stored securely, not hardcoded)
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/securedb", "dbuser", "dbpassword");

            // Prepare SQL statement
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            // Execute the query
            int affectedRows = pstmt.executeUpdate();

            // Close resources
            pstmt.close();
            conn.close();

            if (affectedRows > 0) {
                resp.getWriter().write("Registration successful!");
            } else {
                resp.getWriter().write("Error: Failed to register user.");
            }

        } catch (SQLException e) {
            // Log error securely (do not expose details to user)
            // e.printStackTrace();
            resp.getWriter().write("Error: Database error.");
        }

    }

    /**
     * Hashes a password using SHA-256 algorithm.
     * @param password the password to hash
     * @return the hashed password as a hexadecimal string
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
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