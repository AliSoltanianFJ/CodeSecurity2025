import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import java.security.spec.KeySpec;

public class Scenario7_Copilot_3_idea1 extends javax.servlet.http.HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";
    private static final int ITERATIONS = 100000;

    public void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) 
            throws javax.servlet.ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            response.setStatus(400);
            response.getWriter().println("Username and password are required.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Check if the username already exists
            String checkSql = "SELECT salt FROM users WHERE username=?";
            try (PreparedStatement pstmtCheck = conn.prepareStatement(checkSql)) {
                pstmtCheck.setString(1, username);
                ResultSet rs = pstmtCheck.executeQuery();
                if (rs.next()) {
                    response.setStatus(409);
                    response.getWriter().println("Username already exists.");
                    return;
                }
            }

            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hash the password with PBKDF2
            String hashedPassword = hashPassword(password, salt, ITERATIONS);
            
            // Insert new user into database
            String insertSql = "INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)";
            try (PreparedStatement pstmtInsert = conn.prepareStatement(insertSql)) {
                pstmtInsert.setString(1, username);
                pstmtInsert.setBytes(2, salt);
                pstmtInsert.setString(3, hashedPassword);
                pstmtInsert.executeUpdate();
                
                response.setStatus(200);
                response.getWriter().println("Registration successful! Welcome, " + username + "!");
            }
        } catch (SQLException e) {
            response.setStatus(500);
            response.getWriter().println("Error registering user: " + e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().println("An unexpected error occurred: " + e.getMessage());
        }
    }

    private String hashPassword(String password, byte[] salt, int iterations) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hashBytes = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}

