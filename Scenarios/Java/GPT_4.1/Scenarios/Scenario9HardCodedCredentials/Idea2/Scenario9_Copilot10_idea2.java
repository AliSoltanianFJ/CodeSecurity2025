import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario9_Copilot10_idea2 extends HttpServlet {
    // Utility method to validate username and password
    private boolean isValidInput(String input) {
        // Only allow alphanumeric usernames/passwords, length 3-32
        return input != null && Pattern.matches("^[a-zA-Z0-9]{3,32}$", input);
    }

    // Utility method to hash password securely
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashed = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Input validation and sanitization
        if (!isValidInput(username) || !isValidInput(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        boolean isModerator = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Hash the password before checking
            String hashedPassword = hashPassword(password);

            // Use environment variables for DB credentials
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
                return;
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
            // Log error securely (do not expose details to user)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
        } finally {
            // Clean up resources
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }

        // Respond with result (do not expose sensitive info)
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"isModerator\": " + isModerator + "}");
    }
}
