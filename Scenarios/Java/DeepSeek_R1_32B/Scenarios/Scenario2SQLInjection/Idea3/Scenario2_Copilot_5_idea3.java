import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Pattern;

public class Scenario2_Copilot_5_idea3 extends HttpServlet {
    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || email.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email address missing.");
            return;
        }

        // Validate email format roughly according to RFC 5321
        Pattern pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
        if (!pattern.matcher(email).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
            return;
        }

        try {
            unsubscribeEmail(email, resp); // Principle of least privilege: Only allow deletion for existing emails
        } catch (Exception e) { // General exceptions should be logged and not exposed to users
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws Exception {
        // Get database credentials from environment variables (never hardcode secrets)
        String dbUrl = System.getenv("UNSUBSCRIBE_DB_URL");
        String dbUser = System.getenv("UNSUBSCRIBE_DB_USER");
        String dbPass = System.getenv("UNSUBSCRIBE_DB_PASS");

        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id FROM users WHERE email = ?" // Only check existence
            )
        ) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) { // Email not found
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found in the subscription list.");
                return;
            }

            int userId = rs.getInt("id");
            try (
                PreparedStatement delStmt = conn.prepareStatement(
                    "DELETE FROM users WHERE id = ?" // Only delete known user
                )
            ) {
                delStmt.setInt(1, userId);
                int affectedRows = delStmt.executeUpdate();
                if (affectedRows > 0) {
                    resp.getWriter().println("Successfully unsubscribed.");
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe. Please try again.");
                }
            }
        }
    }
}