import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Random;
import java.util.Properties;
import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DpskR132B_Scenario2_Copilot7_FT_TOSP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot7_FT_TOSP.class.getName());

    // Load database credentials securely (e.g., from a properties file)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("db_credentials.properties")) {
            props.load(in);
            DB_USER = props.getProperty("db.user");
            DB_PASSWORD = props.getProperty("db.password");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database credentials", e);
        }
    }

    // Securely generate a random token
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder();

    private String generateToken(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return BASE64_ENCODER.encodeToString(randomBytes);
    }

    // Validate email (basic check, adjust as needed)
    private boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && email.length() <= 254 && email.contains("@");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        // Validate input
        if (!isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws IOException {
        try (Connection conn = getConnection()) {
            String token = generateToken(32);

            // Use parameterized query to prevent SQL injection
            String sql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        resp.getWriter().write("Email not found in the database.");
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    int userId = rs.getInt("id");

                    // Update the unsubscribe token
                    String updateSql = "UPDATE users SET unsubscribe_token = ? WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, token);
                        updateStmt.setInt(2, userId);
                        updateStmt.executeUpdate();

                        // Do not expose sensitive information
                        resp.getWriter().write("Successfully requested unsubscription. Please check your email for further instructions.");
                        resp.setStatus(HttpServletResponse.SC_OK);
                    }
                }
            }
        }
    }

    private Connection getConnection() throws IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            throw new IOException("Database connection error", e);
        }
    }
}
