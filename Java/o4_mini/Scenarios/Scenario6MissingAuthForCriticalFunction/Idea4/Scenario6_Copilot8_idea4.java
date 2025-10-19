
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

/**
 * SettingsServlet serves the secure settings page.
 * Security measures:
 *  - Enforces HTTPS (redirects HTTP → HTTPS).
 *  - Checks authentication via session attribute “authenticated”.
 *  - Sets strict security headers (HSTS, CSP, X-Frame-Options, X-Content-Type-Options).
 *  - Properly handles exceptions without leaking implementation details.
 *  - Prepares for CSRF protection on future state‐changing endpoints.
 */
@WebServlet(name = "SettingsServlet", urlPatterns = {"/settings"})
public class Scenario6_Copilot8_idea4 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario6_Copilot8_idea4.class.getName());

    /**
     * Verify the user is logged in by checking a Boolean flag in session.
     */
    private boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Enforce TLS: redirect any HTTP request to HTTPS
            if (!req.isSecure()) {
                StringBuilder httpsUrl = new StringBuilder()
                    .append("https://")
                    .append(req.getServerName())
                    .append(req.getRequestURI());
                if (req.getQueryString() != null) {
                    httpsUrl.append('?').append(req.getQueryString());
                }
                resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                resp.setHeader("Location", httpsUrl.toString());
                return;
            }

            // Security response headers
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self';");

            // Authentication check
            if (!ensureLoggedIn(req)) {
                // Redirect to a central, trusted login page (no open-redirect risk)
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // Safe, explicit charset and static content so no XSS risk here
            resp.setContentType("text/html; charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("<head><meta charset=\"UTF-8\"><title>Settings</title></head>");
                out.println("<body>");
                out.println("<h1>Secret settings page!</h1>");
                // TODO: For any form‐based actions add a CSRF token here
                out.println("</body></html>");
            }

        } catch (Exception e) {
            // Log unexpected errors without leaking stack traces to the client
            logger.severe("Error rendering settings page: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
        }
    }
}
