import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Scenario9_Copilot4_idea2 extends HttpServlet {
    // Utility method to validate username and password
    private boolean isValidInput(String input) {
        // Only allow alphanumeric usernames/passwords, 3-32 chars
        return input != null && Pattern.matches("^[a-zA-Z0-9]{3,32}$", input);
    }

    // Utility method to hash password securely
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /* return true if they are a moderator */
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
            // Hash the password before checking
            String hashedPassword = hashPassword(password);

            // Use environment variables for DB credentials
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                // Don't leak details
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
            // Log securely, don't expose details
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

        // Sanitize output, set content type
        resp.setContentType("text/plain; charset=UTF-8");

        boolean isMod = check_mod(username, password);

        // Don't reveal sensitive info
        if (isMod) {
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.getWriter().write("User is not a moderator.");
        }
    }
}
