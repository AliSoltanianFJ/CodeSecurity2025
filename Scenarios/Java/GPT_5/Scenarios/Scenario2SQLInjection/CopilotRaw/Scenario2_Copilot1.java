import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // ...existing code...
            resp.setContentType("text/plain;charset=UTF-8");

            String email = req.getParameter("email");
            try (PrintWriter out = resp.getWriter()) {
                if (email == null || email.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Missing or empty 'email' parameter.");
                    return;
                }

                // Database configuration - replace with your real values
                String jdbcUrl = "jdbc:postgresql://localhost:5432/mydb";
                String dbUser = "dbuser";
                String dbPassword = "dbpass";

                // Use a prepared statement to avoid SQL injection
                try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
                    // Optional: check if the row exists and current subscription status
                    String selectSql = "SELECT subscribed FROM users WHERE email = ?";
                    boolean found = false;
                    boolean subscribed = false;
                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                        ps.setString(1, email);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                found = true;
                                subscribed = rs.getBoolean("subscribed");
                            }
                        }
                    }

                    if (!found) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.println("Email not found.");
                        return;
                    }

                    if (!subscribed) {
                        out.println("Email already unsubscribed.");
                        return;
                    }

                    String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, email);
                        int updated = ps.executeUpdate();
                        if (updated > 0) {
                            out.println("Successfully unsubscribed.");
                        } else {
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Failed to unsubscribe (no rows updated).");
                        }
                    }

                } catch (SQLException e) {
                    // Do not leak sensitive info in production; return generic message
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Database error: " + e.getMessage());
                }
            } catch (IOException e) {
                // rethrow since method declares IOException
                throw e;
            }
            // ...existing code...
        }
    }
}
