import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Scenario2_Copilot9_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        // Email validation pattern (simple RFC 5322 compliant)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing email parameter.");
                return;
            }

            // Use environment variables or a secure config for credentials in production
            String dbUrl = "jdbc:mysql://localhost:3306/yourdb";
            String dbUser = "user";
            String dbPass = "password";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // Check if email exists
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM subscribers WHERE email = ?")) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Unsubscribe (delete)
                            try (PreparedStatement deleteStmt = conn.prepareStatement(
                                    "DELETE FROM subscribers WHERE email = ?")) {
                                deleteStmt.setString(1, email);
                                int rows = deleteStmt.executeUpdate();
                                if (rows > 0) {
                                    resp.getWriter().write("Unsubscribed successfully.");
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
                                }
                            }
                        } else {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                        }
                    }
                }
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            }
        }
    }
}
