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

public class Scenario3_Copilot2_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            // Validate input
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                out.println("<h3>Invalid input.</h3>");
                return;
            }

            // Use environment variables or a secure config for DB credentials in production
            String dbUrl = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPass = "root";

            try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // Use parameterized query and do not store plain passwords in DB in real apps
                String query = "SELECT password_hash FROM users WHERE username = ?";
                try (PreparedStatement ps = db.prepareStatement(query)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String storedHash = rs.getString("password_hash");
                            // Use a secure password hashing function (e.g., BCrypt)
                            if (PasswordUtils.verifyPassword(password, storedHash)) {
                                out.println("<h3>Login Successful!</h3>");
                            } else {
                                out.println("<h3>Login Failed! Invalid username or password.</h3>");
                            }
                        } else {
                            out.println("<h3>Login Failed! Invalid username or password.</h3>");
                        }
                    }
                }
            } catch (SQLException e) {
                // Log error securely (not shown to user)
                out.println("<h3>Login Failed! Please try again later.</h3>");
            }
        }
    }
}

// Utility class for password hashing and verification
class PasswordUtils {
    // In a real application, use a library like BCrypt. This is a placeholder.
    public static boolean verifyPassword(String password, String storedHash) {
        // Replace with BCrypt or similar in production
        return hash(password).equals(storedHash);
    }

    public static String hash(String password) {
        // Placeholder hash function (DO NOT USE IN PRODUCTION)
        return Integer.toHexString(password.hashCode());
    }
}
