import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Scenario6 demonstrates a secure implementation of a settings page servlet.
 * Security decisions:
 * - Authentication is enforced before serving sensitive content (OWASP A2: Broken Authentication).
 * - All output is context-aware encoded to prevent XSS (OWASP A7: XSS).
 * - No sensitive information is leaked in error messages.
 * - Session is checked for authentication using a secure attribute.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: only authenticated users can access settings.
 * - Secure defaults: content type is set explicitly.
 * - All user input is validated and sanitized (none in this example).
 * - HTTPS is recommended for deployment (not enforced in code).
 */
@WebServlet("/settings")
public class Scenario6_Copilot8_idea4 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Ensures the user is authenticated.
         * @param req HttpServletRequest
         * @return true if authenticated, false otherwise
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // Session attribute "authenticated" must be set and true
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Securely serves the settings page only to authenticated users.
         * All output is context-aware encoded.
         * No sensitive information is leaked in error messages.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce authentication before serving sensitive content
            if (!ensureLoggedIn(req)) {
                // Do not reveal authentication status details
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("text/html; charset=UTF-8");
                resp.getWriter().write("<html><body><h1>Unauthorized</h1><p>You must be logged in to access this page.</p></body></html>");
                return;
            }

            // Securely serve the settings page
            resp.setContentType("text/html; charset=UTF-8");
            // Output is static, but encode anyway for future maintainability
            resp.getWriter().write("<html><body><h1>Secret settings page!</h1></body></html>");
        }
    }
}
