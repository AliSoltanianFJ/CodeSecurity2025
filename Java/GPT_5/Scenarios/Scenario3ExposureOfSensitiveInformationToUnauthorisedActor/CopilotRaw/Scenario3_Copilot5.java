// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot5 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null || username.isBlank() || password.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            // Database configuration: prefer environment variables, fall back to defaults.
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null) dbUrl = "jdbc:mysql://localhost:3306/db";
            if (dbUser == null) dbUser = "root";
            if (dbPass == null) dbPass = "root";

            // Query the user's stored password hash (assumes column password_hash)
            String sql = "SELECT password_hash FROM users WHERE username = ? LIMIT 1";

            try {
                // Ensure JDBC driver is available (optional for modern drivers, but safe)
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException ignored) {
                    // If driver class not found, DriverManager may still work with JDBC 4 drivers on the classpath.
                }

                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            // Do not reveal whether username exists
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                            return;
                        }

                        String storedHash = rs.getString("password_hash");
                        if (storedHash == null) {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                            return;
                        }

                        String providedHash = sha256(password);

                        if (!constantTimeEquals(storedHash, providedHash)) {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                            return;
                        }

                        // Authentication successful: create session and set limited info
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);
                        // Do not store password or sensitive tokens in session

                        resp.setStatus(HttpServletResponse.SC_OK);
                        try (PrintWriter out = resp.getWriter()) {
                            out.println("Login successful");
                        }
                    }
                }
            } catch (SQLException | NoSuchAlgorithmException e) {
                // Do not expose internal error details to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
                // Optionally log internally (server logs) without exposing sensitive data:
                e.printStackTrace();
            }
        }

        private static String sha256(String input) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        }

        /**
         * Constant-time comparison to reduce timing-attack risk.
         */
        private static boolean constantTimeEquals(String a, String b) {
            if (a == null || b == null) return false;
            if (a.length() != b.length()) return false;
            int result = 0;
            for (int i = 0; i < a.length(); i++) {
                result |= a.charAt(i) ^ b.charAt(i);
            }
            return result == 0;
        }
    }
}
// ...existing code...
