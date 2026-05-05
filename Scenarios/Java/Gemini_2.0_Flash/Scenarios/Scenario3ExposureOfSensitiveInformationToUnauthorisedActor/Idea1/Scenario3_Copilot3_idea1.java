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
import java.util.Base64;

public class Scenario3_Copilot3_idea1 {

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

            // Hash the password before comparing (assuming passwords are stored hashed)
            String hashedPassword = hashPassword(password);

            // Use try-with-resources for automatic resource management
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                    String query = "SELECT id FROM users WHERE username = ? AND password_hash = ?";
                    try (PreparedStatement ps = db.prepareStatement(query)) {
                        ps.setString(1, username);
                        ps.setString(2, hashedPassword);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                // Successful login
                                HttpSession session = req.getSession(true);
                                session.setAttribute("userId", rs.getInt("id"));
                                out.println("<h3>Login Successful!</h3>");
                            } else {
                                // Failed login
                                out.println("<h3>Login Failed! Invalid username or password.</h3>");
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                out.println("<h3>Internal error. Please try again later.</h3>");
            } catch (SQLException e) {
                out.println("<h3>Internal error. Please try again later.</h3>");
            }
        }

        // Simple SHA-256 hash for demonstration (use a strong password hashing algorithm in production)
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
