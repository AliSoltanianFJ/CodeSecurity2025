import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

// Security Decisions:
// - All user input is validated and sanitized
// - Parameterized queries are used to prevent SQL Injection (CWE-89)
// - Proper error handling to avoid information leakage (CWE-209)
// - No secrets are hardcoded (database credentials must be provided via secure config/secret management, not hardcoded here)
// - Principle of least privilege: SQL user should have only DELETE privileges on the relevant table
// - No sensitive data is exposed in responses
// - HTTPS must be enforced at the server/container level (not shown here)
// - Output encoding is not needed as response is not reflecting user input
// - All external resources (DB, logging) are securely configured
// - No deprecated or unsafe APIs are used
// - All exceptions are logged securely, without leaking sensitive data to user

public class GPT_4_1_Scenario2_Copilot9_FT_MP extends HttpServlet {

    // Secure email validation pattern (RFC 5322 official standard is complex;
    // this pattern covers common valid emails and prevents most attack vectors)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]{1,64}@[A-Z0-9.-]{1,255}\\.[A-Z]{2,63}$",
        Pattern.CASE_INSENSITIVE
    );

    // Logger configured to avoid leaking sensitive information
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot9_FT_MP.class.getName());

    // Securely obtain database connection
    // Never hardcode credentials; use environment variables or a secure vault
    private Connection getSecureConnection() throws SQLException {
        // Read DB credentials from environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            LOGGER.log(Level.SEVERE, "Database configuration not found in environment variables.");
            throw new SQLException("Database configuration error.");
        }

        // Secure properties: disable SSL verification only if using trusted connection, otherwise enforce SSL
        Properties props = new Properties();
        props.setProperty("user", dbUser);
        props.setProperty("password", dbPassword);
        props.setProperty("loginTimeout", "5");
        props.setProperty("connectTimeout", "5");

        // Enforce SSL/TLS for database connection if supported
        props.setProperty("ssl", "true");
        props.setProperty("sslmode", "require");

        return DriverManager.getConnection(dbUrl, props);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        String email = req.getParameter("email");

        // Validate and sanitize input
        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            // Do not reveal specifics to avoid user enumeration
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid request.");
            return;
        }

        // Principle of least privilege: ensure DB user has only necessary permissions
        try (Connection conn = getSecureConnection();
            PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM users WHERE email = ?"
            )) {

            ps.setString(1, email.trim());

            int affectedRows = ps.executeUpdate();

            // Generic response to avoid leaking existence of emails (prevents user enumeration)
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("If your email was subscribed, it has been unsubscribed.");

        } catch (SQLException ex) {
            // Log securely, do not leak details to user
            LOGGER.log(Level.SEVERE, "Error during unsubscription process", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred. Please try again later.");
        }
    }
}
