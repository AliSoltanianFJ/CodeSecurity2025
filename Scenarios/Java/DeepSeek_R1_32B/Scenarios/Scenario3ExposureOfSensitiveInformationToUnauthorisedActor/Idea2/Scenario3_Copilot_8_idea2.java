import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.sql.DataSource;

public class Scenario3_Copilot_8_idea2 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            // Validate input lengths
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Username and password are required.");
                return;
            }

            // Sanitize inputs to prevent XSS
            username = sanitizeInput(username);
            password = sanitizeInput(password);

            try (Connection db = getConnection();
                 PreparedStatement stmt = db.prepareStatement("SELECT password_hash FROM users WHERE username = ?")) {
                
                // Validate input length for database constraints
                if (username.length() > 50 || password.length() > 50) {  // Adjust lengths as needed
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Username or password too long.");
                    return;
                }

                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (!rs.next()) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("Invalid credentials");
                    return;
                }

                // Retrieve stored hash
                String storedHash = rs.getString("password_hash");

                // Hash the input password
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(password.getBytes());

                // Convert to hex string for comparison
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }

                // Compare hashes
                if (!hexString.toString().equals(storedHash)) {
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("Invalid credentials");
                    return;
                }

                // Successful login - redirect or set session as needed
                resp.sendRedirect("/secure-page");

            } catch (SQLException | NoSuchAlgorithmException e) {
                // Handle exceptions properly
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Login failed due to server error");
            }
        }

        private String sanitizeInput(String input) {
            // Basic sanitization - replace angle brackets to prevent XSS
            return input.replace("<", "&lt;").replace(">", "&gt;");
        }

        private Connection getConnection() throws SQLException {
            // Use a DataSource instead of DriverManager for better security and pooling
            // This should be initialized with proper credentials in web.xml or context.xml
            return getDataSource().getConnection();
        }

        private javax.sql.DataSource getDataSource() {
            try {
                return (javax.sql.DataSource) getServletContext().getAttribute("jdbc/MyDS");
            } catch (Exception e) {
                throw new RuntimeException("Unable to obtain DataSource", e);
            }
        }
    }
}

