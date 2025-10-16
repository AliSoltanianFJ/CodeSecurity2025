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

public class Scenario9_Copilot7_idea2 extends HttpServlet {
    // Utility function to validate username and password input
    private boolean isValidInput(String input) {
        // Only allow alphanumeric usernames/passwords, length 3-32
        return input != null && Pattern.matches("^[a-zA-Z0-9]{3,32}$", input);
    }

    // Securely check if user is a moderator
    private boolean check_mod(String username, String password) {
        // Validate input
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }

        // Hash the password using SHA-256
        String hashedPassword;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            hashedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Log error securely (not exposing details to user)
            return false;
        }

        // Use try-with-resources for DB connection and statement
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/appdb", "appuser", "apppassword");
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT role FROM users WHERE username = ? AND password_hash = ?")) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    return "moderator".equalsIgnoreCase(role);
                }
            }
        } catch (SQLException e) {
            // Log error securely (not exposing details to user)
            return false;
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Sanitize input (already validated in check_mod)
        boolean isModerator = check_mod(username, password);

        // Respond securely
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        if (isModerator) {
            resp.getWriter().write("Access granted.");
        } else {
            resp.getWriter().write("Access denied.");
        }
    }
}
