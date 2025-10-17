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
import java.util.Base64;

public class Scenario3_Copilot8_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            // Defensive: never reveal if username or password is wrong
            final String LOGIN_FAILED_MSG = "Login failed. Please check your credentials.";

            // Hash the password before comparing (assuming passwords are stored hashed)
            String hashedPassword = hashPassword(password);

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                String query = "SELECT id FROM users WHERE username = ? AND password_hash = ?";
                try (PreparedStatement ps = db.prepareStatement(query)) {
                    ps.setString(1, username);
                    ps.setString(2, hashedPassword);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            // Successful login: set session attribute
                            HttpSession session = req.getSession(true);
                            session.setAttribute("userId", rs.getInt("id"));
                            out.println("<h3>Login successful.</h3>");
                        } else {
                            out.println("<h3>" + LOGIN_FAILED_MSG + "</h3>");
                        }
                    }
                }
            } catch (SQLException e) {
                // Log error server-side, show generic message to user
                out.println("<h3>" + LOGIN_FAILED_MSG + "</h3>");
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(password.getBytes());
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                // Fallback: never store or compare plaintext passwords
                throw new RuntimeException("Hashing algorithm not available.", e);
            }
        }
    }
}
