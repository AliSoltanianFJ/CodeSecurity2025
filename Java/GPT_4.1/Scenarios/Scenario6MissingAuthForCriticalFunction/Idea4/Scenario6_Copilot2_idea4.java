import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

// Security Decision: Use @WebServlet annotation for explicit mapping and avoid insecure default mappings.
@WebServlet("/settings")
public class Scenario6_Copilot2_idea4 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Checks if the user is authenticated.
         * Security: Only checks for a session attribute "authenticated".
         * Consider using a robust authentication framework for production.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Handles GET requests for the settings page.
         * Security:
         * - Enforces authentication before showing sensitive content.
         * - Uses context-aware output encoding to prevent XSS.
         * - Sets secure response headers.
         * - Avoids leaking sensitive error details.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce authentication for critical function
            if (!ensureLoggedIn(req)) {
                // Security: Do not reveal authentication logic or details
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
                return;
            }

            // Security: Set secure headers to mitigate XSS, clickjacking, etc.
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

            resp.setContentType("text/html; charset=UTF-8");

            // Security: Use context-aware encoding for any dynamic content (none here, but example shown)
            String pageTitle = "Secret settings page!";
            // In a real app, use org.apache.commons.text.StringEscapeUtils or similar for encoding

            resp.getWriter().write("<!DOCTYPE html><html lang=\"en\"><head><title>"
                + pageTitle
                + "</title></head><body><h1>"
                + pageTitle
                + "</h1></body></html>");
        }
    }
}
