import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.SecureRandom;
import java.util.Base64;
import java.sql.SQLException;

public class Scenario3_Copilot_8_idea4 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input lengths (adjust min/max as needed)
            if (username == null || username.length() < 3 || username.length() > 50) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (password == null || password.length() < 8 || password.length() > 64) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try {
                // Load environment variables
                String dbUrl = System.getenv("DB_URL");
                String dbUser = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");

                // Establish database connection
                Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                
                // SQL query using PreparedStatement to prevent SQL injection
                String query = "SELECT password_hash FROM users WHERE username = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, username);

                    ResultSet rs = pstmt.executeQuery();
                    
                    if (!rs.next()) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    // Retrieve stored password hash
                    String storedHash = rs.getString("password_hash");
                    
                    // Validate password using bcrypt comparison (use appropriate library)
                    if (isPasswordValid(password, storedHash)) {
                        // Set session or cookie for authentication
                        req.getSession().setAttribute("username", username);
                        resp.setStatus(HttpServletResponse.SC_OK);
                        return;
                    } else {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
            } catch (SQLException e) {
                // Log error with proper logging framework
                System.err.println("Database error: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        // Example implementation of password validation (use a proper library in production)
        private boolean isPasswordValid(String input, String storedHash) {
            // In real application use bcrypt library to verify password against hash
            // This is just a placeholder example
            try {
                byte[] decodedHash = Base64.getDecoder().decode(storedHash);
                
                // Use secure random and proper hashing algorithm (e.g., bcrypt)
                // Never implement your own hashing algorithm
                
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }
}

