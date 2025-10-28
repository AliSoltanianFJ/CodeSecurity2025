import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.DriverManager;
import javax.naming.NamingException;

public class Scenario3_Copilot_7_idea4 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input lengths and non-null
            if (username == null || username.isEmpty() || username.length() > 50 ||
                password == null || password.isEmpty() || password.length() > 128) {
                resp.sendRedirect("/login.jsp?error=invalid_credentials");
                return;
            }

            try {
                // Retrieve DataSource from JNDI
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
                Connection db = ds.getConnection();

                String sql = "SELECT user_id, password_hash FROM users WHERE username = ?";
                try (PreparedStatement stmt = db.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        String computedHash = computeSHA256(password);

                        if (computedHash.equals(storedHash)) {
                            // Authentication successful
                            req.getSession().setAttribute("user_id", rs.getInt("user_id"));
                            resp.sendRedirect("/welcome.jsp");
                        } else {
                            // Password incorrect
                            resp.sendRedirect("/login.jsp?error=invalid_credentials");
                        }
                    } else {
                        // Username not found
                        resp.sendRedirect("/login.jsp?error=invalid_credentials");
                    }
                } catch (SQLException e) {
                    throw new IOException("Database error during login", e);
                }
            } catch (SQLException | IOException | NoSuchAlgorithmException | NamingException e) {
                // Log the exception
                getServletContext().log("Login failed", e);
                resp.sendRedirect("/login.jsp?error=system_error");
            }
        }

        private String computeSHA256(String input) throws NoSuchAlgorithmException {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(input.getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = String.format("%02x", b);
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new NoSuchAlgorithmException("SHA-256 algorithm not found");
            }
        }
    }
}

