import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot10_idea2 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            try {
                Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                try {
                    // Validate and sanitize input
                    if (username == null || password == null || username.length() > 50 || password.length() > 50) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
                        db.close();
                        return;
                    }
                    username = username.replaceAll("[^a-zA-Z0-9_@.]", "");
                    password = password.replaceAll("[^a-zA-Z0-9!@#$%^&*()_+=-]", "");

                    // Use prepared statements to prevent SQL injection
                    try (PreparedStatement stmt = db.prepareStatement(
                            "SELECT password_hash FROM users WHERE username = ?")) {
                        stmt.setString(1, username);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String storedHash = rs.getString("password_hash");
                                // Use a secure password hashing algorithm (e.g., BCrypt)
                                if (BCrypt.checkpw(password, storedHash)) {
                                    // Set session securely
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    resp.getWriter().write("Login successful.");
                                } else {
                                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                                }
                            } else {
                                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                            }
                        }
                    }
                    db.close();
                } catch (SQLException e) {
                    db.close();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection error.");
            }
        }
    }
}
