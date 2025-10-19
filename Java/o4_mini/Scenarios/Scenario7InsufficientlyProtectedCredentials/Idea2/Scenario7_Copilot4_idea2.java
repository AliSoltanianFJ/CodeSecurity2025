import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@WebServlet("/register")
public class Scenario7_Copilot4_idea2 extends HttpServlet {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Retrieve and trim inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        // 2. Validate inputs
        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password format.");
            return;
        }

        // 3. Attempt to register user
        try {
            registerUser(username, password);
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Log full error on the server, but send generic message to client
            getServletContext().log("Registration error for user " + username, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed. Please try again later.");
            return;
        }

        // 4. Respond success
        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><meta charset=\"UTF-8\"><title>Success</title></head><body>");
            out.println("<h1>Registration successful</h1>");
            out.println("</body></html>");
        }
    }

    private boolean isValidUsername(String username) {
        // Only letters, digits, underscore. Length 3–20.
        return username != null && username.matches("^[A-Za-z0-9_]{3,20}$");
    }

    private boolean isValidPassword(String password) {
        // Length 8–64, no control characters
        return password != null
                && password.length() >= 8
                && password.length() <= 64
                && password.chars().noneMatch(ch -> Character.isISOControl(ch));
    }

    private void registerUser(String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {

        // 1. Generate a new random salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(salt);

        // 2. Hash the password with PBKDF2
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();

        // 3. Encode to Base64 for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 4. Obtain DB credentials from environment (never hard-code)
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        // 5. Store user record using a prepared statement
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();
        }
    }
}
