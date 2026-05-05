// ...existing code...
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

public class Scenario2_Copilot6 {

    public static class UnsubscribeServlet extends HttpServlet {
        // ...existing code...

        // Database connection settings - update to match your environment
        private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC";
        private static final String DB_USER = "dbuser";
        private static final String DB_PASS = "dbpassword";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = resp.getWriter();

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Missing or empty 'email' parameter.");
                return;
            }
            email = email.trim();

            // Load JDBC driver (safe no-op on modern JVMs if driver auto-registered)
            try {
                Class.forName(JDBC_DRIVER);
            } catch (ClassNotFoundException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                getServletContext().log("JDBC Driver not found", e);
                out.println("Server configuration error (JDBC driver).");
                return;
            }

            String selectSql = "SELECT subscribed FROM users WHERE email = ?";
            String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                // Check if email exists and subscription status
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            out.println("Email not found.");
                            return;
                        }
                        boolean subscribed = rs.getBoolean("subscribed");
                        if (!subscribed) {
                            out.println("Email is already unsubscribed.");
                            return;
                        }
                    }
                }

                // Perform unsubscribe
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, email);
                    int rows = psUpdate.executeUpdate();
                    if (rows > 0) {
                        out.println("Successfully unsubscribed.");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Failed to unsubscribe (no rows updated).");
                    }
                }

            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                getServletContext().log("Database error while unsubscribing", e);
                out.println("Database error. Please try again later.");
            }
        }
        // ...existing code...
    }
}
