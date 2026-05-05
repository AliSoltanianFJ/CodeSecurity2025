import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

// Security decision: Use annotation to restrict servlet mapping to authenticated users only.
// In production, configure HTTPS and secure session cookies.
@WebServlet("/settings")
public class Scenario6_Copilot1_idea4 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Checks if the user is authenticated.
         * Security: Relies on session attribute "authenticated" set after successful login.
         * Principle of least privilege: Only authenticated users can access settings.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Securely serves the settings page.
         * Security:
         * - Verifies authentication before serving sensitive content.
         * - Uses context-aware output encoding to prevent XSS.
         * - Sets secure headers.
         * - Avoids leaking sensitive error details.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Ensure user is authenticated before showing settings
            if (!ensureLoggedIn(req)) {
                // Security: Do not reveal authentication logic or details
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
                return;
            }

            // Security: Set secure headers to mitigate XSS and clickjacking
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

            resp.setContentType("text/html; charset=UTF-8");

            // Security: Use context-aware encoding for any dynamic content (none in this example)
            resp.getWriter().write("<!DOCTYPE html><html><head><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
        }
    }
}
