import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Secure Settings page servlet.
 * - Enforces authentication and HTTPS
 * - Sets secure headers (HSTS, CSP, X-Frame-Options, etc.)
 * - Performs proper error handling without leaking internal details
 * - Encodes all user‐controlled data in redirects
 */
@WebServlet(name = "SettingsServlet", urlPatterns = {"/settings"})
public class Scenario6_Copilot3_idea4 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Verify that the user is logged in.
     * @param req the HTTP request
     * @return true if there is a session with a Boolean “authenticated” flag set to true
     */
    private boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            // 1. Authentication check
            if (!ensureLoggedIn(req)) {
                HttpSession session = req.getSession(false);
                if (session != null) {
                    session.invalidate();  // Prevent session fixation
                }
                // Redirect to login page
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // 2. Enforce HTTPS
            if (!req.isSecure()) {
                StringBuilder httpsUrl = new StringBuilder("https://")
                        .append(req.getServerName())
                        .append(req.getRequestURI());
                String qs = req.getQueryString();
                if (qs != null && !qs.isEmpty()) {
                    // Encode user‐controlled query component
                    httpsUrl.append("?")
                            .append(URLEncoder.encode(qs, StandardCharsets.UTF_8.name()));
                }
                resp.sendRedirect(httpsUrl.toString());
                return;
            }

            // 3. Security headers
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            resp.setHeader("Content-Security-Policy", "default-src 'self'");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "SAMEORIGIN");
            resp.setHeader("Referrer-Policy", "no-referrer");
            resp.setHeader("Permissions-Policy", "geolocation=(), microphone=()");
            resp.setContentType("text/html; charset=UTF-8");

            // 4. Render page (no user‐supplied data in body to avoid XSS)
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("<head><meta charset=\"UTF-8\"><title>Settings</title></head>");
                out.println("<body>");
                out.println("<h1>Secret settings page!</h1>");
                out.println("</body>");
                out.println("</html>");
            }

        } catch (Exception e) {
            // Log internally, but do not expose stack trace or sensitive info
            log("Failed to process settings page", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An internal error occurred. Please try again later.");
        }
    }
}
