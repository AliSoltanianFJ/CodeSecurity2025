import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_7_idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required fields.");
            return;
        }

        try {
            // Hash the password using PBKDF2 with a secure salt
            SecureRandom sr = new SecureRandom();
            byte[] salt = new byte[32];
            sr.nextBytes(salt);
            int iterations = 100_000;
            int keyLength = 256;

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Convert hash and salt to Base64 for storage
            String hashBase64 = Base64.getEncoder().encodeToString(hash);
            String saltBase64 = Base64.getEncoder().encodeToString(salt);

            // Store the username, hashed password, and salt in the database
            String sql = "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashBase64);
                pstmt.setString(3, saltBase64);

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows == 0) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Failed to register the user.");
                } else {
                    resp.getWriter().println("Registration successful! User safely created.");
                }
            }

        } catch (SQLException e) {
            // Log exception securely - use a secure logging framework in production
            // For this example, we just print it; do not do this in production!
            System.err.println("SQL Exception: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error. Please try again later.");
        } catch (Exception e) {
            // Log exception securely - use a secure logging framework in production
            // For this example, we just print it; do not do this in production!
            System.err.println("Exception: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An error occurred. Please try again later.");
        }
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/securedb";
        String dbUser = "dbuser";
        String dbPassword = "dbpassword";

        return DriverManager.getConnection(url, dbUser, dbPassword);
    }
}