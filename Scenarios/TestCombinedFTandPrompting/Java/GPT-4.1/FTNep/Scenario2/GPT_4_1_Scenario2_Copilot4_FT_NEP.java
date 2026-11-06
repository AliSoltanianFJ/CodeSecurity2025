import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Pattern;


/**
 * SecureUnsubscribeServlet handles unsubscribe requests securely.
 * All sensitive information is handled via environment variables.
 * No secrets are hardcoded. Email input is strictly validated and sanitized.
 * Error messages are generic, and detailed info is only logged server-side.
 * Follows OWASP guidelines for secure coding.
 */
public class GPT_4_1_Scenario2_Copilot4_FT_NEP extends HttpServlet {

    // Strict email validation pattern (OWASP recommendation)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,255}\\.[A-Za-z]{2,10}$"
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Get email parameter and trim whitespace
        String email = req.getParameter("email");
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
            return;
        }
        email = email.trim();

        // Principle of least privilege: Only use a dedicated, restricted DB account
        try (Connection conn = DatabaseUtils.getConnection();
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT id FROM subscribers WHERE email = ?"
            );
            PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM subscribers WHERE email = ?"
            )
        ) {
            checkStmt.setString(1, email);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Unsubscribe
                    deleteStmt.setString(1, email);
                    int rows = deleteStmt.executeUpdate();
                    if (rows > 0) {
                        resp.getWriter().write("You have been unsubscribed.");
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process request.");
                    }
                } else {
                    // Do not reveal if email exists, to prevent enumeration
                    resp.getWriter().write("You have been unsubscribed.");
                }
            }
        } catch (Exception e) {
            // Log detailed error server-side only, never expose stack trace to user
            SecurityLogger.log("Unsubscribe error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process request.");
        }
    }
}
