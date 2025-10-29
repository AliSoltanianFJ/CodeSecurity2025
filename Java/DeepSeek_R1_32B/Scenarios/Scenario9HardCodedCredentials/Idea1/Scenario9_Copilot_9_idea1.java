import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class Scenario9_Copilot_9_idea1 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);
        resp.getWriter().append(String.valueOf(isModerator));
    }

    private boolean check_mod(String username, String password) {
        try (Connection conn = getConnection()) {
            String query = "SELECT password_hash, role FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                
                if (!rs.next()) {
                    return false;
                }
                
                String storedHash = rs.getString("password_hash");
                String role = rs.getString("role");

                // Assuming password_hash is a salted hash
                String inputHash = hashWithSalt(password, extractSalt(storedHash));
                
                return "moderator".equals(role) && inputHash.equals(storedHash);
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Handle any SQL or crypto errors
            System.err.println("Error checking moderator credentials: " + e.getMessage());
            return false;
        }
    }

    private String extractSalt(String storedHash) {
        // Implementation depends on how the salt is stored in password_hash
        // Example: If the hash is formatted as {salt}{hash}, split and return the salt part
        return "";
    }

    private String hashWithSalt(String input, String salt) throws NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] digest = md.digest(input.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() throws SQLException {
        // Use environment variables or secure configuration
        String url = "jdbc:mysql://localhost:3306/yourdatabase";
        String user = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "username";
        String password = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "password";
        return DriverManager.getConnection(url, user, password);
    }
}

