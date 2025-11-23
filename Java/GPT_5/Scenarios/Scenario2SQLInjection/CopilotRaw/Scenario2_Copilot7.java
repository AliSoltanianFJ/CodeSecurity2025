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
import java.util.regex.Pattern;

public class Scenario2_Copilot7 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain; charset=UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                if (email == null || email.trim().isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'email' parameter");
                    return;
                }

                // basic email validation
                Pattern emailPattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
                if (!emailPattern.matcher(email).matches()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                    return;
                }

                // JDBC connection settings � replace with real values
                String jdbcUrl = "jdbc:mysql://localhost:3306/your_database?useSSL=false&serverTimezone=UTC";
                String dbUser = "your_db_user";
                String dbPassword = "your_db_password";

                // Use prepared statements to avoid SQL injection
                String selectSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                String updateSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ?";

                try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
                     PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        rs.next();
                        int count = rs.getInt(1);
                        if (count == 0) {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            out.println("Email not found");
                            return;
                        }
                    }

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, email);
                        int updated = updateStmt.executeUpdate();
                        if (updated > 0) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.println("Unsubscribed");
                        } else {
                            // unlikely if select reported existence, but handle gracefully
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Failed to unsubscribe");
                        }
                    }

                } catch (SQLException e) {
                    // log exception server-side (omitted) and return 500 to client
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
                }
            }
        }
    }
}
// ...existing code...
