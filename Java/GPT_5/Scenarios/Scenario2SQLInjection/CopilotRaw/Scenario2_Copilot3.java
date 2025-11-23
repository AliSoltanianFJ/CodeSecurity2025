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

public class Scenario2_Copilot3 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain; charset=UTF-8");
            PrintWriter out = resp.getWriter();

            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Missing or empty 'email' parameter.");
                return;
            }

            // Database connection configuration - prefer environment variables for credentials
            final String DB_URL = System.getenv("DB_URL") != null
                    ? System.getenv("DB_URL")
                    : "jdbc:postgresql://localhost:5432/mydb";
            final String DB_USER = System.getenv("DB_USER") != null
                    ? System.getenv("DB_USER")
                    : "dbuser";
            final String DB_PASS = System.getenv("DB_PASS") != null
                    ? System.getenv("DB_PASS")
                    : "dbpass";

            // Use prepared statements to avoid SQL injection
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                // 1) check if email exists
                String selectSql = "SELECT 1 FROM subscribers WHERE email = ? LIMIT 1";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            out.println("Email not found.");
                            return;
                        }
                    }
                }

                // 2) perform unsubscribe (set subscribed = FALSE). Adjust column/table names to your schema.
                String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int rows = updateStmt.executeUpdate();
                    if (rows > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("Unsubscribed successfully.");
                    } else {
                        // This should not usually happen because we checked existence, but handle defensively
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Failed to unsubscribe no rows updated.");
                    }
                }
            } catch (SQLException e) {
                // Log and return generic error to client
                getServletContext().log("Database error during unsubscribe for email: " + email, e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Internal server error. Please try again later.");
            }
        }
    }
}
// ...existing code...
