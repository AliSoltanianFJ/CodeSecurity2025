import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario9_Copilot5_idea2 extends HttpServlet {
    // Validate username and password using a safe regex
    private boolean isValidInput(String input) {
        // Only allow alphanumeric usernames and passwords, length 3-32
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

    // Check if user is a moderator
    private boolean check_mod(String username, String password) {
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }

        String sql = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
        try (
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/appdb", "appuser", "apppassword"
            );
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    return "moderator".equalsIgnoreCase(role);
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log exception securely (not shown here)
            // Do not expose internal errors to the user
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
