import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Scenario9_Copilot_4_idea4 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate inputs
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            if (check_mod(username, password)) {
                resp.getWriter().write("Moderator access granted.");
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (Exception e) {
            // Log exception details
            System.err.println("Error validating moderator credentials: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean check_mod(String username, String password) throws SQLException, IOException {
        // Validate input lengths
        if (username.length() > 50 || username.length() < 3 ||
                password.length() > 50 || password.length() < 8) {
            return false;
        }

        // Use DataSource for secure database connection
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT role, password_hash FROM users WHERE username = ?")) {

            // Sanitize input parameters
            stmt.setString(1, username);
            
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return false; // No user found
            }

            String storedHash = rs.getString("password_hash");
            String role = rs.getString("role");

            // Verify password hash using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            
            String providedHash = bytesToHex(hashBytes);

            return providedHash.equals(storedHash) && "moderator".equals(role);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Error hashing password", e);
        }
    }

    private Connection getConnection() throws SQLException {
        // Use a secure DataSource configuration
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/mydb",
                    "user", "password"); // Never hardcode credentials in production!
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

