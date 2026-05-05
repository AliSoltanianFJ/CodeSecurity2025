import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot5_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                out.println("<h3>Login Failed! Username and password are required.</h3>");
                return;
            }

            // Hash the password before comparing with the stored hash
            String hashedPassword = hashPassword(password);

            // Use try-with-resources for automatic resource management
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                     PreparedStatement stmt = db.prepareStatement(
                         "SELECT password_hash FROM users WHERE username = ?")) {

                    stmt.setString(1, username);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedHash = rs.getString("password_hash");
                            if (storedHash != null && storedHash.equals(hashedPassword)) {
                                // Successful login
                                out.println("<h3>Login Successful!</h3>");
                            } else {
                                out.println("<h3>Login Failed! Invalid username or password.</h3>");
                            }
                        } else {
                            out.println("<h3>Login Failed! Invalid username or password.</h3>");
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "JDBC Driver not found", e);
                out.println("<h3>Login Failed! Internal error.</h3>");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
                out.println("<h3>Login Failed! Internal error.</h3>");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error", e);
                out.println("<h3>Login Failed! Internal error.</h3>");
            } finally {
                out.close();
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                // In production, handle this more gracefully
                throw new RuntimeException("Hashing algorithm not found", e);
            }
        }
    }
}
