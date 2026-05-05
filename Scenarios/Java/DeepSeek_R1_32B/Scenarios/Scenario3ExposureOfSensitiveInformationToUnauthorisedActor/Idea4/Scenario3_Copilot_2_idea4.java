import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;

public class Scenario3_Copilot_2_idea4 {

    public static class LoginServlet extends HttpServlet {
        /* Log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: Username and password are required.");
                return;
            }

            try (Connection db = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/db",
                    System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD"))) {

                String query = "SELECT id, username, password_hash FROM users WHERE username = ?";
                try (PreparedStatement stmt = db.prepareStatement(query)) {
                    stmt.setString(1, username);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            // User not found
                            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            resp.getWriter().write("Invalid credentials.");
                            return;
                        }

                        String storedHash = rs.getString("password_hash");
                        
                        // Verify password using bcrypt
                        if (verifyPassword(password, storedHash)) {
                            // Successful login
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().write("Login successful.");
                            
                            // Store user ID in session for future authentication
                            req.getSession().setAttribute("userId", rs.getInt("id"));
                        } else {
                            // Invalid password
                            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            resp.getWriter().write("Invalid credentials.");
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal server error. Please try again later.");
            }
        }

        private boolean verifyPassword(String plainTextPassword, String storedHash) {
            if (plainTextPassword == null || storedHash == null) {
                return false;
            }

            // Parse the stored hash
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);

            try {
                KeySpec spec = new PBEKeySpec(plainTextPassword.toCharArray(), salt, iterations, 256);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] hash = skf.generateSecret(spec).getEncoded();

                // Convert to hex string for comparison
                return storedHash.equals(iterations + ":" + Base64.getEncoder().encodeToString(hash));
            } catch (Exception e) {
                return false;
            }
        }
    }
}

