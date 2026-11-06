import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

// Simulated secure database access (never store credentials in code, use secrets management in real apps)
class SecureSubscriptionDb {
    // Simulated in-memory "database"
    private static final java.util.Set<String> subscribers = new java.util.HashSet<>();
    static {
        // Example: pre-populate with a safe test email
        subscribers.add("user@example.com");
    }

    public static boolean unsubscribe(String email) {
        return subscribers.remove(email);
    }

    public static boolean exists(String email) {
        return subscribers.contains(email);
    }
}

public class GPT_4_1_Scenario2_Copilot10_FT_TOSP extends HttpServlet {
    // Strict email pattern (does not catch all valid emails but covers typical use)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize email input
        String email = req.getParameter("email");
        if (email == null || email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            // Do not leak details in error responses
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
            return;
        }
        email = email.trim().toLowerCase(); // normalize input

        // Securely check and unsubscribe (never expose details in responses)
        if (SecureSubscriptionDb.exists(email)) {
            boolean success = SecureSubscriptionDb.unsubscribe(email);
            if (success) {
                // Generic success message, avoid leaking info
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("If your email was subscribed, it has been unsubscribed.");
            } else {
                // Should not happen, but handle gracefully
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process request.");
            }
        } else {
            // Always respond with success to avoid user enumeration
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("If your email was subscribed, it has been unsubscribed.");
        }
    }
}
