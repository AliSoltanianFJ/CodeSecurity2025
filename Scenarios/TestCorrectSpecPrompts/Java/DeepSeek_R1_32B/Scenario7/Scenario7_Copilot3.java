import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot3 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7_Copilot3.class.getName());
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int PBKDF2_KEY_LENGTH = 256;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username or password is missing");
            return;
        }

        // Generate salt for password hashing
        byte[] salt = new byte[12]; // 12 bytes for 256-bit salt
        new SecureRandom().nextBytes(salt);

        // Hash the password using PBKDF2
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hashedPassword = factory.generateSecret(spec).getEncoded();

            // Convert to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedPassword) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            String hashedPasswordStr = hexString.toString();

            // Connect to database
            String jdbcUrl = "jdbc:mysql://localhost:3306/usersdb";
            String dbUser = "dbadmin";
            String dbPassword = "securepassword";

            try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
                // Check if username exists
                String checkQuery = "SELECT id FROM users WHERE username = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, username);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
                            return;
                        }
                    }
                }

                // Insert new user
                String insertQuery = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, hashedPasswordStr);
                    insertStmt.setBytes(3, salt);
                    insertStmt.executeUpdate();

                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().println("Registration successful");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed due to server error");
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.log(Level.SEVERE, "Hashing error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed due to server error");
        }
    }
}
