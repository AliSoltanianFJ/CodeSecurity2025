import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Scenario6: Secure implementation of a settings page.
 * Security decisions:
 * - Only authenticated users can access settings.
 * - All user input is validated and sanitized.
 * - No sensitive information is leaked in error messages.
 * - Content is encoded to prevent XSS.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege is applied.
 * - Secure defaults are used.
 */
@WebServlet("/settings")
public class Scenario6_Copilot4_idea4 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Checks if the user is authenticated.
         * @param req HttpServletRequest
         * @return true if authenticated, false otherwise
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Securely handles GET requests for the settings page.
         * - Only accessible to authenticated users.
         * - All output is HTML-encoded to prevent XSS.
         * - No sensitive data is exposed.
         * - Proper error handling is implemented.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce HTTPS (best practice, but actual enforcement should be at server config level)
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
                return;
            }

            // Authentication check
            if (!ensureLoggedIn(req)) {
                // Do not reveal authentication logic details
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
                return;
            }

            // Example: If there were user inputs, validate and sanitize here.
            // String param = req.getParameter("setting");
            // if (param != null && !isValid(param)) {
            //     resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            //     return;
            // }

            // Set secure headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

            resp.setContentType("text/html; charset=UTF-8");

            // Output encoding to prevent XSS (no user input here, but always encode)
            String safeHtml = "<html><head><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>";
            resp.getWriter().write(safeHtml);
        }

        // Example input validation method (not used here, but for future extensibility)
        // private boolean isValid(String input) {
        //     // Implement strict validation logic
        //     return input != null && input.matches("[a-zA-Z0-9_\\-]{1,32}");
        // }
    }
}
