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

public class Scenario3_Copilot4_idea1 {

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
                out.println("<h3>Login Failed! Username and password are required.</h3>");
                return;
            }

            // Hash the password before comparing (assuming passwords are stored hashed)
            String hashedPassword;
            try {
                hashedPassword = hashPassword(password);
            } catch (NoSuchAlgorithmException e) {
                out.println("<h3>Internal error. Please try again later.</h3>");
                return;
            }

            // Use try-with-resources to ensure resources are closed
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                out.println("<h3>Internal error. Please try again later.</h3>");
                return;
            }

            String dbUrl = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPass = "root";
            String query = "SELECT id FROM users WHERE username = ? AND password_hash = ?";

            try (
                Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                PreparedStatement stmt = db.prepareStatement(query)
            ) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Successful login
                        out.println("<h3>Login Successful!</h3>");
                    } else {
                        // Failed login
                        out.println("<h3>Login Failed! Invalid username or password.</h3>");
                    }
                }
            } catch (SQLException e) {
                out.println("<h3>Internal error. Please try again later.</h3>");
            }
        }

        // Simple SHA-256 hash for demonstration (use a strong password hashing algorithm in production)
        private String hashPassword(String password) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}
