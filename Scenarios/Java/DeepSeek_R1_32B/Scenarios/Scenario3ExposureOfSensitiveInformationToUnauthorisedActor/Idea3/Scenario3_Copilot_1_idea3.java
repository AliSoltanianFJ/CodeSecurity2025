import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;


public class Scenario3_Copilot_1_idea3 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario3_Copilot_1_idea3.class.getName());

    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            login(username, password, resp);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during login", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed due to a server error.");
        }
    }

    private void login(String username, String password, HttpServletResponse resp)
            throws SQLException, NoSuchAlgorithmException, IOException {

        // Validate input
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Principle of least privilege: Only allow alphanumeric usernames
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        // Securely hash the password with a strong salt
        byte[] salt = getSalt();
        String hashedPassword = hashPassword(password, salt);

        // Principle of least privilege: Only allow SHA-256 hashes for password comparison
        if (!hashedPassword.matches("^[a-fA-F0-9]{64}$")) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid password format.");
            return;
        }

        // Securely retrieve database credentials
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUser == null || dbPass == null) {
            // Principle of least privilege: Do not expose credential errors to users
            LOGGER.log(Level.SEVERE, "Database credentials not set in environment variables.");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error.");
            return;
        }

        // Securely connect to database (do not hardcode credentials)
        Connection db = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/db",
                dbUser,
                dbPass
        );

        // Use parameterized query to prevent SQL injection
        String query = "SELECT id FROM users WHERE username = ? AND password = ?";
        PreparedStatement preparedStatement = db.prepareStatement(query);
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, hashedPassword);

        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            // Login successful, start a secure session
            String sessionId = generateSessionId();
            resp.setHeader("Set-Cookie", "session=" + sessionId +
                    "; HttpOnly; Secure; SameSite=Strict");
            resp.getWriter().write("Login successful.");
        } else {
            // Principle of least privilege: Do not leak information about why login failed
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
        }

        resultSet.close();
        preparedStatement.close();
        db.close();
    }

    private byte[] getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return salt;
    }

    private String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hashedBytes = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedBytes);
    }

    private String generateSessionId() {
        SecureRandom sr = new SecureRandom();
        byte[] sessionBytes = new byte[32];
        sr.nextBytes(sessionBytes);
        return Base64.getEncoder().encodeToString(sessionBytes);
    }
}