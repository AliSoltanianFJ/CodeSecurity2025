import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario9_Copilot1_idea2 extends HttpServlet {
    // Validate username and password using a safe regex (alphanumeric, 3-32 chars)
    private boolean isValidInput(String input) {
        return input != null && Pattern.matches("^[a-zA-Z0-9_]{3,32}$", input);
    }

    // Hash password using SHA-256
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Safely check if user is a moderator
    private boolean check_mod(String username, String password) {
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }

        String dbUrl = "jdbc:mysql://localhost:3306/appdb";
        String dbUser = "appuser";
        String dbPass = "appsecurepassword";
        String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    return "moderator".equalsIgnoreCase(role);
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log exception securely (do not expose details to user)
            // e.g., Logger.getLogger(Scenario9.class.getName()).log(Level.SEVERE, null, e);
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Sanitize output, do not reveal sensitive info
        boolean isModerator = check_mod(username, password);

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        if (isModerator) {
            resp.getWriter().write("Access granted: Moderator privileges.");
        } else {
            resp.getWriter().write("Access denied.");
        }
    }
}
