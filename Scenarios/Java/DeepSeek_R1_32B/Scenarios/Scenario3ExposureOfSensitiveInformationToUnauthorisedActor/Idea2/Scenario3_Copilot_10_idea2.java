import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.sql.SQLException;

public class Scenario3_Copilot_10_idea2 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input to prevent SQL injection and other attacks
            if (username == null || password == null || 
                username.trim().isEmpty() || password.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required");
                return;
            }

            try {
                Connection db = DriverManager.getConnection(
                    System.getenv("DB_URL"), 
                    System.getenv("DB_USER"), 
                    System.getenv("DB_PASSWORD")
                );

                String query = "SELECT id, username FROM users WHERE username = ?";
                
                try (PreparedStatement pstmt = db.prepareStatement(query)) {
                    // Sanitize input to prevent SQL injection
                    pstmt.setString(1, username);
                    
                    ResultSet rs = pstmt.executeQuery();
                    
                    if (!rs.next()) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        resp.getWriter().write("Invalid credentials");
                        return;
                    }

                    String storedUsername = rs.getString("username");
                    String storedPasswordHash = null;

                    // Get the password hash from another query
                    String passwordQuery = "SELECT password FROM users WHERE username = ?";
                    try (PreparedStatement pwdPstmt = db.prepareStatement(passwordQuery)) {
                        pwdPstmt.setString(1, username);
                        ResultSet pwdRs = pwdPstmt.executeQuery();
                        if (pwdRs.next()) {
                            storedPasswordHash = pwdRs.getString("password");
                        }
                    }

                    // Verify password hash
                    if (storedPasswordHash == null || !verifyPassword(password, storedPasswordHash)) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        resp.getWriter().write("Invalid credentials");
                        return;
                    }

                    // Set session attributes
                    req.getSession().setAttribute("username", username);

                    // Return success response
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"message\":\"Login successful\"}");
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred");
                throw new IOException(e);
            }
        }

        // Method to verify password against hash using bcrypt or similar algorithm
        private boolean verifyPassword(String plainPassword, String storedHash) {
            try {
                byte[] decodedHash = Base64.getDecoder().decode(storedHash);
                return java.security.MessageDigest.getInstance("SHA-256").digest(plainPassword.getBytes()).equals(decodedHash);
            } catch (Exception e) {
                return false;
            }
        }
    }
}

