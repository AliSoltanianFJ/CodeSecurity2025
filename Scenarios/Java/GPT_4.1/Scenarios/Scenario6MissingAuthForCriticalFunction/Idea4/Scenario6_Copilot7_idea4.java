import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

// Security Decision: Use @WebServlet annotation for explicit mapping and avoid insecure default mappings.
@WebServlet(name = "SettingsServlet", urlPatterns = {"/settings"})
public class Scenario6_Copilot7_idea4 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Ensures the user is logged in by checking for a valid session and authenticated attribute.
         * Security: Principle of least privilege, avoids session fixation, and does not leak session info.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Handles GET requests for the settings page.
         * Security:
         * - Requires authentication for access.
         * - Sets secure response headers to mitigate XSS, clickjacking, and MIME sniffing.
         * - Does not expose sensitive error details.
         * - Content is static and does not reflect user input (prevents XSS).
         * - Assumes HTTPS is enforced at the server/container level.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Require authentication for access to settings
            if (!ensureLoggedIn(req)) {
                // Security: Do not reveal authentication logic or details
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
                return;
            }

            // Security: Set secure headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';");

            resp.setContentType("text/html; charset=UTF-8");

            // Security: Static content, no user input reflected
            resp.getWriter().write("<!DOCTYPE html><html lang=\"en\"><head><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
        }
    }
}
