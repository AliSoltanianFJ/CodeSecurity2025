import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Scenario6: Secure implementation of a settings page servlet.
 * Security decisions:
 * - Authentication is strictly enforced before serving sensitive content.
 * - All user inputs are validated and sanitized (none in this example, but template provided).
 * - Content-Type is set explicitly.
 * - Error handling avoids leaking sensitive details.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: only authenticated users can access settings.
 * - Output is context-aware encoded (no user input reflected here).
 * - HTTPS should be enforced at deployment (not shown in code).
 */
@WebServlet("/settings")
public class Scenario6_Copilot6_idea4 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Checks if the user is authenticated.
         * @param req HttpServletRequest
         * @return true if authenticated, false otherwise
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // Defensive: session attribute should be Boolean and true
            Object auth = (session != null) ? session.getAttribute("authenticated") : null;
            return (auth instanceof Boolean) && ((Boolean) auth);
        }

        /**
         * Securely serves the settings page only to authenticated users.
         * All user inputs should be validated and encoded before use.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce authentication for critical function
            if (!ensureLoggedIn(req)) {
                // Log access attempt securely (do not leak details to user)
                // Optionally: log IP, timestamp, etc. for audit (not shown here)
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("text/html; charset=UTF-8");
                resp.getWriter().write("<html><body><h2>Unauthorized access</h2></body></html>");
                return;
            }

            // No user input reflected; if there were, use context-aware encoding
            resp.setContentType("text/html; charset=UTF-8");
            resp.getWriter().write("<html><body><h1>Secret settings page!</h1></body></html>");
        }
    }
}
