import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario7_Copilot_5_idea3 extends HttpServlet {

// Email regex pattern
private static final String EMAIL_REGEX = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

// Password regex pattern:
// - at least 8 characters long
// - contains at least one uppercase letter
// - contains at least one lowercase letter
// - contains at least one digit
// - contains at least one special character (e.g., !@#$%)
private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");
    String email = req.getParameter("email");

    if (username == null || username.length() < 3 || username.length() > 50) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
        return;
    }

    if (password == null || !isValidPassword(password)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password.");
        return;
    }

    if (email == null || !isValidEmail(email)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
        return;
    }

    String hashedPassword = hashPassword(password);
    try {
        register(username, hashedPassword, email, resp);
    } catch (SQLException e) {
        // Log the exception securely
        System.err.println("SQL Exception: " + e.getMessage());
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed due to a server error.");
    }
}

private void register(String username, String hashedPassword, String email, HttpServletResponse resp) throws SQLException, IOException {
    String dbUrl = System.getenv("DB_URL");
    String dbUser = System.getenv("DB_USER");
    String dbPass = System.getenv("DB_PASSWORD");

    if (dbUrl == null || dbUser == null || dbPass == null) {
        System.err.println("Database credentials not configured.");
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed due to server configuration.");
        return;
    }

    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
         PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)")) {
        ps.setString(1, username);
        ps.setString(2, hashedPassword);
        ps.setString(3, email);
        int rowsAffected = ps.executeUpdate();
        if (rowsAffected > 0) {
            resp.getWriter().write("Registration successful.");
        } else {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    } catch (SQLException e) {
        // Log the exception securely
        System.err.println("SQL Exception: " + e.getMessage());
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed due to a server error.");
    }
}

private String hashPassword(String password) {
    if (password == null || password.length() < 8) {
        throw new IllegalArgumentException("Password must be at least 8 characters long.");
    }

    try {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    } catch (NoSuchAlgorithmException e) {
        // Log the exception securely
        System.err.println("SHA-512 algorithm not found: " + e.getMessage());
        return null; // Or throw a custom exception
    }
}

private boolean isValidEmail(String email) {
    if (email == null || email.isEmpty()) {
        return false;
    }
    Pattern pattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(email);
    return matcher.matches();
}

private boolean isValidPassword(String password) {
    if (password == null || password.length() < 8) {
        return false;
    }
    Pattern pattern = Pattern.compile(PASSWORD_REGEX);
    Matcher matcher = pattern.matcher(password);
    return matcher.matches();
}

}