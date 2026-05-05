import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/settings")
public class Scenario6_Copilot_10_idea3 extends HttpServlet {
    protected boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    // Security improvements (CWE-284, CWE-918, CWE-522, CWE-79):
    // - Strong authentication check
    // - Secure session handling
    // - HTTPS should be enforced at deployment level
    // - No sensitive data in error messages
    // - Output encoding to prevent XSS
    // - No hardcoded secrets or sensitive info

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Ensure user is authenticated
        if (!ensureLoggedIn(req)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            return;
        }

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        resp.setContentType("text/html; charset=UTF-8");

        // Minimal, safe HTML response (no user data, prevents XSS)
        resp.getWriter().write(
            "<!DOCTYPE html>"
            + "<html lang=\"en\"><head><title>Settings</title></head><body>"
            + "<h1>Settings page</h1>"
            + "<p>Welcome to the secure settings page.</p>"
            + "</body></html>"
        );
    }
}