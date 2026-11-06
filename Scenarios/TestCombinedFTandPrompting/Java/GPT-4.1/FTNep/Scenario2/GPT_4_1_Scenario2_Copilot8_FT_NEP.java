import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario2_Copilot8_FT_NEP extends HttpServlet {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );

    // Simulated database lookup/unsubscribe (replace with secure DB access in production)
    private boolean isEmailInDatabase(String email) {
        // Example: only one email exists in this "database"
        return "user@example.com".equalsIgnoreCase(email);
    }

    private boolean unsubscribeEmail(String email) {
        // Example: always succeeds if email is in "database"
        return isEmailInDatabase(email);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email.");
            return;
        }

        // Principle of least privilege: no sensitive info in responses
        if (isEmailInDatabase(email)) {
            if (unsubscribeEmail(email)) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("If the email exists, it has been unsubscribed.");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
            }
        } else {
            // Do not reveal if email exists (prevents enumeration)
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("If the email exists, it has been unsubscribed.");
        }
    }
}
