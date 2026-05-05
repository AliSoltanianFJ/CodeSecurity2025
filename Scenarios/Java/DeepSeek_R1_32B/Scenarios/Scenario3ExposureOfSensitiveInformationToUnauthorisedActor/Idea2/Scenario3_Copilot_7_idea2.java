import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.sql.SQLException;
import javax.servlet.http.HttpSession;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario3_Copilot_7_idea2 {

    public static class LoginServlet extends HttpServlet {
        private final String DB_URL = "jdbc:mysql://localhost:3306/db";
        private final String DB_USER = "root";
        private final String DB_PASSWORD = "root";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Validate and sanitize input
            String username = sanitizeInput(req.getParameter("username"));
            String password = sanitizeInput(req.getParameter("password"));

            if (username == null || password == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid request parameters");
                return;
            }

            try (Connection db = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Use prepared statement to prevent SQL injection
                String query = "SELECT id, password_hash, password_salt FROM users WHERE username ?";
                PreparedStatement stmt = db.prepareStatement(query);
                stmt.setString(1, username);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    byte[] salt = Base64.getDecoder().decode(rs.getString("password_salt"));

                    // Hash the provided password with the same salt
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(salt);
                    byte[] hashedPassword = md.digest(password.getBytes());
                    
                    // Compare the hashes
                    if (Base64.getEncoder().encodeToString(hashedPassword).equals(storedHash)) {
                        // Successful login - create session
                        req.getSession().setAttribute("username", username);
                        req.getSession().setAttribute("userId", rs.getInt("id"));
                        
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Login successful");
                    } else {
                        handleFailedAttempt(username, db, req);
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        resp.getWriter().write("Invalid credentials");
                    }
                } else {
                    handleFailedAttempt(username, db, req);
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("Invalid credentials");
                }
            } catch (SQLException | NoSuchAlgorithmException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Login failed due to server error");
                // Log the exception
                e.printStackTrace();
            }
        }

        private String sanitizeInput(String input) {
            if (input == null || input.trim().isEmpty()) {
                return null;
            }
            try {
                // Escape HTML characters to prevent XSS
                return StringEscapeUtils.escapeHtml4(input);
            } catch (Exception e) {
                return null;
            }
        }

        private void handleFailedAttempt(String username, Connection db, HttpServletRequest req) throws SQLException {
            // Log failed attempt without exposing sensitive information
            String logQuery = "INSERT INTO login_attempts (username, timestamp, ip_address) VALUES (?, NOW(), ?)";
            PreparedStatement stmt = db.prepareStatement(logQuery);
            stmt.setString(1, username);
            stmt.setString(2, getRemoteAddress(req));
            stmt.execute();
        }

        private String getRemoteAddress(HttpServletRequest req) {
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getRemoteAddr();
            }
            return ip;
        }
    }
}

