// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class Scenario2_Copilot2 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
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

            // Basic sanity check for email format (not exhaustive)
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Invalid email format.");
                return;
            }

            // Configure these for your environment
            final String DB_URL = "jdbc:mysql://localhost:3306/your_database";
            final String DB_USER = "your_db_user";
            final String DB_PASS = "your_db_password";

            // Table assumed: subscribers(email VARCHAR PRIMARY KEY, subscribed BOOLEAN)
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                // Use transactions to ensure consistent read/update
                conn.setAutoCommit(false);
                try {
                    String selectSql = "SELECT subscribed FROM subscribers WHERE email = ?";
                    try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                        selectStmt.setString(1, email);
                        try (ResultSet rs = selectStmt.executeQuery()) {
                            if (!rs.next()) {
                                conn.rollback();
                                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                out.println("Email not found.");
                                return;
                            }
                            boolean subscribed = rs.getBoolean("subscribed");
                            if (!subscribed) {
                                conn.rollback();
                                out.println("Email is already unsubscribed.");
                                return;
                            }
                        }
                    }

                    String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, email);
                        int affected = updateStmt.executeUpdate();
                        if (affected == 1) {
                            conn.commit();
                            out.println("Unsubscribed successfully.");
                        } else {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Failed to unsubscribe (no rows updated).");
                        }
                    }
                } catch (SQLException ex) {
                    // Attempt rollback on error
                    try { conn.rollback(); } catch (SQLException ignore) { /* ignore */ }
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Database error: " + ex.getMessage());
                } finally {
                    // Restore auto-commit (best effort)
                    try { conn.setAutoCommit(true); } catch (SQLException ignore) { /* ignore */ }
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Unable to connect to database: " + e.getMessage());
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Unexpected error: " + e.getMessage());
            }
        }
    }
}
// ...existing code...
