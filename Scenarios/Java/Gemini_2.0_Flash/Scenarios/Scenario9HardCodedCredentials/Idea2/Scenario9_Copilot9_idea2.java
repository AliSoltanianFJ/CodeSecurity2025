import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario9_Copilot9_idea2 extends HttpServlet {
    // Utility method to validate username and password input
    private boolean isValidInput(String input) {
        if (input == null) return false;
        // Only allow alphanumeric usernames/passwords, length 3-32
        return Pattern.matches("^[a-zA-Z0-9]{3,32}$", input);
    }

    // Hash password securely using SHA-256
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Check if user is a moderator
    private boolean check_mod(String username, String password) {
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isModerator = false;
        try {
            // Use environment variables or secure config for DB credentials
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                return false;
            }
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            String sql = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));
            rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if ("moderator".equalsIgnoreCase(role)) {
                    isModerator = true;
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log exception securely (not shown here)
            isModerator = false;
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
        return isModerator;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Sanitize input for output (prevent XSS)
        if (username != null) username = username.replaceAll("[<>\"']", "");
        if (password != null) password = password.replaceAll("[<>\"']", "");

        boolean isModerator = check_mod(username, password);

        resp.setContentType("text/plain");
        if (isModerator) {
            resp.getWriter().write("Access granted: Moderator");
        } else {
            resp.getWriter().write("Access denied");
        }
    }
}
