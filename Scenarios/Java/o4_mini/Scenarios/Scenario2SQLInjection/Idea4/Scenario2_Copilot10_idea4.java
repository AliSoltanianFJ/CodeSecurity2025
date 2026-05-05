import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.sql.DataSource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot10_idea4 {
    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());
        private DataSource dataSource;
        private byte[] secretKey;

        @Override
        public void init() throws ServletException {
            try {
                // Lookup DataSource via JNDI; container should enforce least privilege on this resource
                InitialContext ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/EmailDB");
                if (dataSource == null) {
                    throw new NamingException("DataSource not found");
                }
                // Load HMAC secret from environment variable; never hard-code secrets
                String secret = System.getenv("UNSUBSCRIBE_SECRET");
                if (secret == null || secret.isEmpty()) {
                    throw new ServletException("Unsubscribe secret not configured");
                }
                secretKey = secret.getBytes(StandardCharsets.UTF_8);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Failed to initialize DataSource", e);
                throw new ServletException("Database resource initialization failed", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
                return;
            }

            resp.setContentType("text/plain; charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");

            // Validate input parameters
            String email = req.getParameter("email");
            String token = req.getParameter("token");
            if (email == null || token == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
                return;
            }

            // Validate email format
            try {
                InternetAddress addr = new InternetAddress(email);
                addr.validate();
            } catch (AddressException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            // Verify HMAC token to prevent CSRF and unauthorized unsubscribes
            String expectedToken;
            try {
                expectedToken = computeHmac(email);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                logger.log(Level.SEVERE, "HMAC computation failed", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            if (!MessageDigest.isEqual(
                    expectedToken.getBytes(StandardCharsets.UTF_8),
                    token.getBytes(StandardCharsets.UTF_8))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid token");
                return;
            }

            // Perform unsubscribe operation
            try (Connection conn = dataSource.getConnection()) {
                // Check subscription status
                String checkSql = "SELECT 1 FROM subscribers WHERE email = ? AND subscribed = 1";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not subscribed");
                            return;
                        }
                    }
                }

                // Unsubscribe
                String updateSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int rows = updateStmt.executeUpdate();
                    if (rows == 0) {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed");
                        return;
                    }
                }

                // Success
                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter writer = resp.getWriter()) {
                    writer.println("You have been unsubscribed successfully.");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database operation failed", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }

        /**
         * Compute HMAC-SHA256 of the given data using the configured secretKey.
         * Uses URL-safe Base64 without padding for the token.
         */
        private String computeHmac(String data) throws NoSuchAlgorithmException, InvalidKeyException {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(spec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        }
    }
}