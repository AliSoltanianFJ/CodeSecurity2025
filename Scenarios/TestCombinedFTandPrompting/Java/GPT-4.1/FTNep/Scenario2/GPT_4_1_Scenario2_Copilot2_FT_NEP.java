import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// All DB operations are delegated to a secure DAO class. 
// No credentials or secrets are hardcoded.
// Secrets must be stored using the deployment/secret manager, not in source.
// Only whitelisted, well-formed emails are processed.

public class GPT_4_1_Scenario2_Copilot2_FT_NEP extends HttpServlet {
    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1. Validate and sanitize email input
        String email = req.getParameter("email");
        if (email == null || email.length() > 254 || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email parameter.");
            return;
        }

        // 2. Principle of least privilege: Only call DAO, do not handle credentials here
        try {
            if (SecureSubscriberDAO.unsubscribe(email)) {
                resp.getWriter().write("Unsubscribed successfully.");
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found or already unsubscribed.");
            }
        } catch (Exception e) {
            // Log securely (never leak sensitive info), here just returning generic error
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
        }
    }

    // Secure, strict email validation (defense in depth)
    private boolean isValidEmail(String email) {
        // RFC 5322 official standard regex is overly complex; use a safe subset
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$");
    }
}

