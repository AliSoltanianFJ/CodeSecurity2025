import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot4_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {
        // Simple regex for demonstration; consider using a robust library in production
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || (email = email.trim()).isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'email' parameter");
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            // In a real app, load these from env vars or a secure config
            String dbUrl  = System.getenv("DB_URL");    // e.g. "jdbc:postgresql://localhost:5432/mydb"
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            // JDBC logic
            try (
                Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                // First, check if the email exists and is currently subscribed
                PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT subscribed FROM subscribers WHERE email = ?"
                )
            ) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        // no such email
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                        return;
                    }
                    boolean currentlySubscribed = rs.getBoolean("subscribed");
                    if (!currentlySubscribed) {
                        resp.sendError(HttpServletResponse.SC_CONFLICT, "Email already unsubscribed");
                        return;
                    }
                }

                // Email exists and is subscribed → unsubscribe
                try (PreparedStatement unsubStmt = conn.prepareStatement(
                        "UPDATE subscribers SET subscribed = FALSE, unsubscribed_at = CURRENT_TIMESTAMP WHERE email = ?"
                     )) {
                    unsubStmt.setString(1, email);
                    int rows = unsubStmt.executeUpdate();
                    if (rows != 1) {
                        // unexpected
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                       "Failed to update subscription status");
                        return;
                    }
                }

                // Success
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write("You have been unsubscribed successfully.");

            } catch (SQLException sqle) {
                // Log the exception somewhere (not to the user)
                sqle.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "A database error occurred.");
            }
        }
    }
}
