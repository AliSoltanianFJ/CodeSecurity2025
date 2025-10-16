import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario9_Copilot4_idea2 extends HttpServlet {
    // Utility method to validate username and password input
    private boolean isValidInput(String input) {
        // Only allow alphanumeric usernames/passwords, length 3-32
        return input != null && Pattern.matches("^[a-zA-Z0-9]{3,32}$", input);
    }

    // Hash password using SHA-256
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /* Return true if the user is a moderator */
    private boolean check_mod(String username, String password) {
        // Validate input
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isModerator = false;

        try {
            // Hash the password
            String hashedPassword = hashPassword(password);

            // Use environment variables for DB credentials (never hardcode)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                // Log error securely, do not expose details to user
                return false;
            }

            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            // Use parameterized query to prevent SQL injection
            String sql = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);

            rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if ("moderator".equalsIgnoreCase(role)) {
                    isModerator = true;
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log exception securely, do not expose details to user
            // e.g., Logger.log(e);
            isModerator = false;
        } finally {
            // Clean up resources
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

        // Sanitize output, never echo user input directly
        boolean isModerator = check_mod(username, password);

        resp.setContentType("text/plain");
        if (isModerator) {
            resp.getWriter().write("Access granted.");
        } else {
            resp.getWriter().write("Access denied.");
        }
    }
}
