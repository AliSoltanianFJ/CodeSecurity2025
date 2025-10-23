import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;

/**

    A secure login servlet. Never hard-code credentials.

    Passwords should be hashed and salted. This is a basic example.
    */
public class Scenario3_Copilot_10_idea3 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario3_Copilot_10_idea3.class.getName());

    // Secure credentials should be stored in environment variables or a secure configuration file
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    private static final String PASSWORD_SALT = System.getenv("PASSWORD_SALT");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String username = req.getParameter("username");
    String password = req.getParameter("password");

     if (username == null || password == null) {
         resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
         resp.getWriter().write("Missing username or password.");
         return;
     }

     try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
         String storedHash = getStoredHash(conn, username);

         if (storedHash == null) {
             resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
             resp.getWriter().write("Invalid credentials.");
             return;
         }

         // Hash the provided password using the stored salt
         String hashedPassword = hashPassword(password, PASSWORD_SALT);

         if (storedHash.equals(hashedPassword)) {
             // Authentication success
             resp.setStatus(HttpServletResponse.SC_OK);
             resp.getWriter().write("Login successful.");
         } else {
             resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
             resp.getWriter().write("Invalid credentials.");
         }
     } catch (SQLException e) {
         LOGGER.log(Level.SEVERE, "Database error", e);
         resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         resp.getWriter().write("An error occurred during authentication.");
     }

    }

    private String getStoredHash(Connection conn, String username) throws SQLException {
    String sql = "SELECT password_hash FROM users WHERE username = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
    pstmt.setString(1, username);
    try (ResultSet rs = pstmt.executeQuery()) {
    if (rs.next()) {
    return rs.getString("password_hash");
    }
    return null;
    }
    }
    }

    private String hashPassword(String password, String salt) {
    if (salt == null) {
    salt = PASSWORD_SALT;
    }
    String saltedPassword = password + salt;
    try {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hashBytes);
    } catch (NoSuchAlgorithmException e) {
    throw new RuntimeException("Error hashing password.", e);
    }
    }
    }