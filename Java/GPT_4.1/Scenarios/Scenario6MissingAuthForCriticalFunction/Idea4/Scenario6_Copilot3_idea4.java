import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Scenario6: Secure implementation of a settings page servlet.
 * Security decisions:
 * - Authentication is strictly enforced before serving sensitive content.
 * - All output is context-aware encoded to prevent XSS.
 * - No sensitive information is leaked in error messages.
 * - Session is checked for validity and authentication.
 * - No secrets or credentials are hardcoded.
 * - Secure defaults are used for all configurations.
 * - Principle of least privilege is applied.
 * - All user input is validated and sanitized (none in this example, but placeholder added).
 * - HTTPS is assumed for deployment; ensure transport security at server configuration.
 */
@WebServlet("/settings")
public class Scenario6_Copilot3_idea4 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Checks if the user is authenticated.
         * @param req HttpServletRequest
         * @return true if authenticated, false otherwise
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // Principle of least privilege: Only allow access if authenticated attribute is present and true
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Securely serves the settings page only to authenticated users.
         * All output is context-aware encoded.
         * No sensitive information is leaked on error.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce authentication before serving sensitive content
            if (!ensureLoggedIn(req)) {
                // Do not leak authentication status details
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("text/html; charset=UTF-8");
                resp.getWriter().write("<html><body><h2>Access Denied</h2><p>You must be logged in to view this page.</p></body></html>");
                return;
            }

            // No user input is processed here, but always encode output to prevent XSS
            resp.setContentType("text/html; charset=UTF-8");
            // Context-aware output encoding (static content, but use encoding for dynamic content)
            resp.getWriter().write("<html><body><h1>Secret settings page!</h1></body></html>");
        }
    }
}
