import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DpskR132B_Scenario7_Copilot3_FT_MP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input: Ensure username and password are not null or empty
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.getWriter().write("Error: Username and password are required.");
            return;
        }

        // Principle of least privilege: Use a dedicated database user with minimal privileges
        // Secure configuration: Never hardcode credentials; use environment variables or secure vaults
        // In practice, use try-with-resources for resource management
        String dbUrl = "jdbc:mysql://localhost:3306/securedb"; // Secure configuration: Use HTTPS for database connections if possible
        String dbUser = "db_user"; // Secure configuration: Use environment variables for credentials
        String dbPassword = "db_password"; // Secure configuration: Use environment variables for credentials

        try {
            // Use parameterized queries to prevent SQL injection (CWE-89)
            // Principle of least privilege: Only insert, not select or update
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Secure password storage: Hash password with SHA-256 and a salt (CWE-321)
                // Note: In production, use a cryptographically secure random salt and a stronger hash function like bcrypt or PBKDF2
                String hashedPassword = hashPassword(password, "12345678901234567890"); // Secure configuration: Use a secure random salt

                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);

                int rowsInserted = pstmt.executeUpdate();
                if (rowsInserted > 0) {
                    resp.getWriter().write("Registration successful!");
                } else {
                    resp.getWriter().write("Error: Failed to register.");
                }
            }
        } catch (SQLException e) {
            // Secure error handling: Do not leak sensitive information (CWE-209)
            // Principle of least privilege: Log errors securely, do not expose stack traces to users
            resp.getWriter().write("Error: Database error. Please try again later.");
            // Log the exception securely (do not log sensitive data)
            // Use a secure logging framework in production
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // Secure error handling: Do not leak sensitive information
            resp.getWriter().write("Error: Internal server error.");
            // Log the exception securely
            // Use a secure logging framework in production
            e.printStackTrace();
        }
    }

    /**
     * Hashes the password using SHA-256 with a salt.
     * @param password The password to hash.
     * @param salt The salt to use for hashing.
     * @return The hashed password as a hexadecimal string.
     * @throws NoSuchAlgorithmException If SHA-256 is not available.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        // Secure cryptography: Use SHA-256 with a salt (CWE-321)
        // Note: In production, use a stronger hash function like bcrypt or PBKDF2
        // and a cryptographically secure random salt.
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes("UTF-8"));
            byte[] hashBytes = md.digest(password.getBytes("UTF-8"));

            // Convert hash to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Error hashing password", e);
        }
    }
}
