import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
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

public class Scenario3_Copilot1_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                out.println("<h3>Login Failed! Username and password must not be empty.</h3>");
                return;
            }

            // Hash the password before checking against the database
            String hashedPassword;
            try {
                hashedPassword = hashPassword(password);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.log(Level.SEVERE, "Password hashing failed", e);
                out.println("<h3>Internal error. Please try again later.</h3>");
                return;
            }

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                String query = "SELECT id FROM users WHERE username = ? AND password_hash = ?";
                try (PreparedStatement stmt = db.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, hashedPassword);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            // Successful login, set session attribute
                            HttpSession session = req.getSession(true);
                            session.setAttribute("userId", rs.getInt("id"));
                            out.println("<h3>Login Successful!</h3>");
                        } else {
                            out.println("<h3>Login Failed! Invalid username or password.</h3>");
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
                out.println("<h3>Internal error. Please try again later.</h3>");
            }
        }

        private String hashPassword(String password) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
    }
}
