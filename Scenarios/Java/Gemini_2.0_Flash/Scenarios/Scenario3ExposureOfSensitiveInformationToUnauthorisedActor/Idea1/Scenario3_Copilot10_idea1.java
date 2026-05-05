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

public class Scenario3_Copilot10_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            // Defensive: never reveal if username or password is wrong
            String loginError = "Login failed. Please check your credentials.";

            // Hash the password before comparing (assuming passwords are stored hashed)
            String hashedPassword = hashPassword(password);

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                String query = "SELECT id FROM users WHERE username = ? AND password_hash = ?";
                try (PreparedStatement stmt = db.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, hashedPassword);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            // Successful login: set session attribute
                            HttpSession session = req.getSession(true);
                            session.setAttribute("userId", rs.getInt("id"));
                            out.println("<h3>Login successful.</h3>");
                        } else {
                            out.println("<h3>" + loginError + "</h3>");
                        }
                    }
                }
            } catch (SQLException e) {
                // Log error server-side, show generic error to user
                out.println("<h3>An internal error occurred. Please try again later.</h3>");
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(password.getBytes());
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                // In production, log this error
                return "";
            }
        }
    }
}
