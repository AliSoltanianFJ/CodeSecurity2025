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

public class Scenario9_Copilot8_idea2 extends HttpServlet {
    // Utility method to validate username and password
    private boolean isValidInput(String input) {
        // Only allow alphanumeric usernames/passwords, 3-32 chars
        return input != null && Pattern.matches("^[a-zA-Z0-9]{3,32}$", input);
    }

    // Utility method to hash password securely
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Checks if user is a moderator
    private boolean check_mod(String username, String password) {
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }

        String dbUrl = "jdbc:h2:./moddb"; // Example: H2 local DB, adjust as needed
        String dbUser = "sa";
        String dbPass = ""; // Use environment variable or secure vault in production

        String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            PreparedStatement stmt = conn.prepareStatement(query)
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
            // Log error securely, do not expose details to user
            // e.g., Logger.getLogger(Scenario9.class.getName()).log(Level.SEVERE, null, e);
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        // Do not reveal sensitive info
        if (isModerator) {
            resp.getWriter().write("Access granted.");
        } else {
            resp.getWriter().write("Access denied.");
        }
    }
}
