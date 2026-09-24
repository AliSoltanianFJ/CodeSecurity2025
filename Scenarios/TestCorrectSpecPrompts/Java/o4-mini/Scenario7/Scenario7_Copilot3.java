import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario7_Copilot3 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7_Copilot3.class.getName());

    // PBKDF2 parameters
    private static final int SALT_LENGTH = 16;       // bytes
    private static final int ITERATIONS  = 65536;
    private static final int KEY_LENGTH  = 256;      // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Simple parameter validation
        if (username == null || username.isEmpty() ||
            password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Username and password are required");
            return;
        }

        try {
            // 1) Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // 2) Derive the key (the password hash) via PBKDF2
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // 3) Base64-encode for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 4) Read JDBC settings from environment (don’t hardcode!)
            String dbUrl      = System.getenv("DB_URL");
            String dbUser     = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                logger.severe("Database credentials not configured in environment");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Server configuration error");
                return;
            }

            // 5) Store the user record
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)")) {

                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.setInt(4, ITERATIONS);
                ps.executeUpdate();
            }

            // 6) Success
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("text/plain");
            try (PrintWriter w = resp.getWriter()) {
                w.println("User registered successfully");
            }

        } catch (Exception e) {
            // Log full stack trace on the server
            logger.log(Level.SEVERE, "Error during user registration", e);
            // Return a generic error to the client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "An internal error occurred");
        }
    }
}