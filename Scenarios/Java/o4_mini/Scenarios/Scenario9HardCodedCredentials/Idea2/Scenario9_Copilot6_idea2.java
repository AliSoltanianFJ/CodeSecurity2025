package Scenarios.Scenario9HardCodedCredentials;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/scenario9")
public class Scenario9_Copilot6_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Only allow usernames 3–50 chars, letters, digits or underscore
    private static final String USERNAME_REGEX = "^[A-Za-z0-9_]{3,50}$";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
            return;
        }

        try {
            if (checkMod(username, password)) {
                // static text only—no user input is echoed back
                resp.getWriter().write("Access granted.");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            }

        } catch (SQLException | NoSuchAlgorithmException | IllegalStateException e) {
            // Log the exception on the server (not shown to the user)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns true if the given username/password belong to a moderator account.
     */
    private boolean checkMod(String username, String password)
            throws SQLException, NoSuchAlgorithmException {

        // 1) Validate/sanitize username
        if (!username.matches(USERNAME_REGEX)) {
            return false;
        }

        // 2) Hash the password using SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexHash = new StringBuilder(2 * digest.length);
        for (byte b : digest) {
            hexHash.append(String.format("%02x", b));
        }
        String passwordHash = hexHash.toString();

        // 3) Get DB credentials from environment (never hard-code secrets!)
        String dbUrl      = System.getenv("DB_URL");      // e.g. "jdbc:postgresql://db.example.com/app"
        String dbUser     = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database credentials are not configured");
        }

        // 4) Use a prepared statement to avoid SQL injection
        String sql = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    return "moderator".equals(role);
                }
            }
        }

        return false;
    }
}
