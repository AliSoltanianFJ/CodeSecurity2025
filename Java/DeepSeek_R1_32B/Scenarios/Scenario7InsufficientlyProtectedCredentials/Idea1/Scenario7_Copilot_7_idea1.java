import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Scenario7_Copilot_7_idea1 extends javax.servlet.http.HttpServlet {

    private static final String DB_URL = "jdbc:hsqldb:mem:testdb";

    protected void doPost(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) throws javax.servlet.ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                throw new IllegalArgumentException("Username and password are required.");
            }

            // Generate a random salt
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hash the password using PBKDF2
            int iterations = 10000;
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // Convert salt and hash to Base64
            String saltStr = Base64.getEncoder().encodeToString(salt);
            String hashStr = Base64.getEncoder().encodeToString(hash);

            // Prepare SQL statement to insert user data
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, hashStr);
                    pstmt.setString(3, saltStr);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows == 1) {
                        resp.getWriter().println("Registration successful!");
                    } else {
                        throw new SQLException("Failed to register user.");
                    }
                }
            }

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Error registering user: " + e.getMessage());
        } catch (Exception e) {
            // Catch other exceptions, such as NoSuchAlgorithmException
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("An error occurred during registration: " + e.getMessage());
        }
    }

    // Ensure that the necessary table is created if not exists
    private void createTableIfNotExists() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "username VARCHAR(255) UNIQUE NOT NULL," +
                         "password_hash VARCHAR(255) NOT NULL," +
                         "salt VARCHAR(255) NOT NULL" +
                         ")";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.executeUpdate();
            }
        }
    }

    public void init() throws ServletException {
        try {
            createTableIfNotExists();
        } catch (SQLException e) {
            throw new ServletException("Failed to initialize database table", e);
        }
    }
}

