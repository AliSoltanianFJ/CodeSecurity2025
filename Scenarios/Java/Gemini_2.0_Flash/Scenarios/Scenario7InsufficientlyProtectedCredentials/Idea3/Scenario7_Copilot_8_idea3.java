import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario7_Copilot_8_idea3 extends HttpServlet {
private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,30}$");
private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.[a-z])(?=.[A-Z])(?=.*\\d)(?=.[@$!%?&])[A-Za-z\\d@$!%?&]{8,100}$");

private String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(salt);
    byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : hash) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}

private byte[] generateSalt() {
    SecureRandom sr = new SecureRandom();
    byte[] salt = new byte[16];
    sr.nextBytes(salt);
    return salt;
}

@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");

    // Validate and sanitize inputs
    if (username == null || password == null ||
        !USERNAME_PATTERN.matcher(username).matches() ||
        !PASSWORD_PATTERN.matcher(password).matches()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password");
        return;
    }

    Connection conn = null;
    PreparedStatement pstmt = null;

    try {
        // Use environment variables for database credentials
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration missing");
            return;
        }

        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        // Check if username already exists
        pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
        pstmt.setString(1, username);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken");
            return;
        }

        // Generate salt and hash password
        byte[] salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);

        // Insert user into database
        pstmt = conn.prepareStatement("INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)");
        pstmt.setString(1, username);
        pstmt.setString(2, hashedPassword);
        pstmt.setBytes(3, salt);
        pstmt.executeUpdate();

        // Registration success
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().println("Registration successful!");

    } catch (SQLIntegrityConstraintViolationException e) {
        resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken");
    } catch (SQLException e) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred");
        e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
    } finally {
        try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
        try { if (conn != null) conn.close(); } catch (SQLException e) {}
    }
}

}