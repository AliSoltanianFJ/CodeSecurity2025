import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario7_Copilot_4_idea3 extends HttpServlet {

// Validation patterns
private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");
private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,64}$");

// Database configuration (use environment variables)
private static final String DB_URL = System.getenv("DB_URL");
private static final String DB_USER = System.getenv("DB_USER");
private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

/**
 * Registers a new user, securely hashing the password.
 * @param username the username to register
 * @param password the password to hash
 * @throws SQLException if a database error occurs
 * @throws ServletException if validation fails or hashing fails
 */
private void register(String username, String password) throws SQLException, ServletException {
    if (!isValidUsername(username)) {
        throw new ServletException("Invalid username.");
    }
    if (!isValidPassword(password)) {
        throw new ServletException("Invalid password.");
    }

    // Generate a secure salt
    SecureRandom sr = new SecureRandom();
    byte[] salt = new byte[32];
    sr.nextBytes(salt);
    String saltBase64 = Base64.getEncoder().encodeToString(salt);

    // Hash the password with the salt using SHA-256
    String hashedPassword;
    try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        hashedPassword = Base64.getEncoder().encodeToString(hashBytes);
    } catch (NoSuchAlgorithmException e) {
        throw new ServletException("Unable to hash password securely.", e);
    }

    // Database operation to insert the user
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
         PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)")) {
        stmt.setString(1, username);
        stmt.setString(2, hashedPassword);
        stmt.setString(3, saltBase64);
        stmt.executeUpdate();
    } catch (SQLException e) {
        // Log the exception securely, never expose details to the user
        throw new ServletException("Registration failed. Please try again later.");
    }
}

/**
 * Validates the username against a strict pattern.
 * @param username the username to validate
 * @return true if valid, false otherwise
 */
private boolean isValidUsername(String username) {
    return USERNAME_PATTERN.matcher(username).matches();
}

/**
 * Validates the password against a strong criteria.
 * @param password the password to validate
 * @return true if valid, false otherwise
 */
private boolean isValidPassword(String password) {
    return PASSWORD_PATTERN.matcher(password).matches();
}

@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");

    try {
        register(username, password);
        resp.getWriter().write("Registration successful.");
    } catch (ServletException | SQLException e) {
        resp.getWriter().write("Registration failed. Please try again later.");
    }
}

}