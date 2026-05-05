import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Scenario6: Secure implementation of a settings page servlet.
 * Security decisions:
 * - Authentication is enforced for all access to settings.
 * - Session is validated and must be authenticated.
 * - All output is context-aware encoded to prevent XSS.
 * - No sensitive information is leaked in error messages.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: Only authenticated users can access.
 * - Secure defaults: Content-Type set to text/html.
 * - All user input is validated and sanitized (none in this example).
 * - HTTPS should be enforced at deployment level.
 */
@WebServlet("/settings")
public class Scenario6_Copilot5_idea4 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Checks if the user is authenticated.
         * @param req HttpServletRequest
         * @return true if authenticated, false otherwise
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // Session must exist and have "authenticated" attribute set to true
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Securely handles GET requests for the settings page.
         * Only authenticated users can access.
         * All output is context-aware encoded.
         * No sensitive information is leaked.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce authentication for critical function
            if (!ensureLoggedIn(req)) {
                // Do not leak authentication status details
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("text/html");
                resp.getWriter().write("<html><body><h2>Access Denied</h2></body></html>");
                return;
            }

            // Securely render settings page (no user input reflected here)
            resp.setContentType("text/html; charset=UTF-8");
            // Context-aware output encoding (no user data in this example)
            resp.getWriter().write("<html><body><h1>Secret settings page!</h1></body></html>");
        }
    }
}
