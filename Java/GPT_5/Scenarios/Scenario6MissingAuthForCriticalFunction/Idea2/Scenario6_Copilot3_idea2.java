// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Safe implementation of the settings servlet.
 * - Validates session authentication strictly (Boolean.TRUE)
 * - Requires a CSRF token header that matches a server-side session token
 * - Uses safe HTTP headers and UTF-8 encoding
 * - Does not echo user input (prevents XSS)
 * - Handles exceptions without leaking sensitive information
 */
public class Scenario6_Copilot3_idea2 {
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Only allow tokens of reasonable length and characters (alphanumeric, - _)
        private static final Pattern CSRF_TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,128}$");

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            if (session == null) {
                return false;
            }
            Object auth = session.getAttribute("authenticated");
            // Require a strict Boolean.TRUE to avoid ambiguous truthy values
            return Boolean.TRUE.equals(auth);
        }

        private boolean isValidCsrfToken(String token) {
            return token != null && CSRF_TOKEN_PATTERN.matcher(token).matches();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Use a limited try/catch to ensure we map any unexpected errors to generic responses
            try {
                // Set secure response headers
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("text/html; charset=UTF-8");
                // Prevent MIME-type sniffing
                resp.setHeader("X-Content-Type-Options", "nosniff");
                // Clickjacking protection
                resp.setHeader("X-Frame-Options", "DENY");
                // Minimal CSP; we serve only our own static HTML (no inline scripts)
                resp.setHeader("Content-Security-Policy", "default-src 'self'");
                resp.setHeader("Referrer-Policy", "no-referrer");

                // Authentication check
                if (!ensureLoggedIn(req)) {
                    // Do not reveal why access was denied
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
                    return;
                }

                // CSRF protection: require header X-CSRF-Token matching the session token
                HttpSession session = req.getSession(false);
                if (session == null) {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
                    return;
                }

                Object sessionTokenObj = session.getAttribute("csrfToken");
                if (!(sessionTokenObj instanceof String)) {
                    // If no token present in session, forbid access for this critical function
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Request not allowed.");
                    return;
                }

                String sessionToken = (String) sessionTokenObj;
                String headerToken = req.getHeader("X-CSRF-Token");

                // Validate token formats before comparing
                if (!isValidCsrfToken(headerToken) || !isValidCsrfToken(sessionToken) || !Objects.equals(headerToken, sessionToken)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Request not allowed.");
                    return;
                }

                // Produce safe HTML output. Do not include any user-controlled data.
                try (PrintWriter out = resp.getWriter()) {
                    out.println("<!DOCTYPE html>");
                    out.println("<html lang=\"en\">");
                    out.println("<head>");
                    out.println("  <meta charset=\"UTF-8\">");
                    out.println("  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">");
                    out.println("  <title>Settings</title>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("  <h1>Secret settings page!</h1>");
                    out.println("  <p>Changes must be performed through the secure API.</p>");
                    out.println("</body>");
                    out.println("</html>");
                    out.flush();
                }

            } catch (IllegalArgumentException iae) {
                // Map validation/parsing problems to a 400 without revealing details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request.");
            } catch (Exception e) {
                // Log the exception server-side without exposing specifics to the client
                try {
                    getServletContext().log("SettingsServlet unexpected error", e);
                } catch (Exception ignored) { }
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            }
        }
    }
}
// ...existing code...
