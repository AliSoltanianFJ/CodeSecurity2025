import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

/**
 * Secure implementation of the settings servlet.
 *
 * Notes:
 * - Validates and sanitizes any request input.
 * - Enforces authentication check.
 * - Sets secure headers.
 * - Avoids exposing internal details on errors.
 * - Uses a small, safe HTML escaping helper to prevent XSS.
 */
public class Scenario6_Copilot5_idea2 {

    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            if (session == null) {
                return false;
            }
            Object attr = session.getAttribute("authenticated");
            return (attr instanceof Boolean) && (Boolean) attr;
        }

        private static String escapeHtml(String s) {
            if (s == null) return "";
            StringBuilder out = new StringBuilder(Math.min(s.length(), 1024));
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '<': out.append("&lt;"); break;
                    case '>': out.append("&gt;"); break;
                    case '&': out.append("&amp;"); break;
                    case '"': out.append("&quot;"); break;
                    case '\'': out.append("&#x27;"); break;
                    case '/': out.append("&#x2F;"); break;
                    default: out.append(c);
                }
            }
            return out.toString();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Authentication check - deny if not authenticated
                if (!ensureLoggedIn(req)) {
                    // Do not reveal why; give a generic response
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication required.");
                    return;
                }

                // Set secure response headers
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("text/html; charset=UTF-8");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Referrer-Policy", "no-referrer");
                // Restrictive CSP: allow only same-origin styles if needed; no scripts
                resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self' 'unsafe-inline';");

                // Example of validated input handling:
                // Permit an optional 'action' parameter but validate against a tight whitelist.
                String rawAction = Optional.ofNullable(req.getParameter("action")).orElse("view").trim();

                // Enforce length limits to avoid excessive memory usage
                if (rawAction.length() == 0 || rawAction.length() > 50) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter.");
                    return;
                }

                // Whitelist pattern: alphanumerics, dash, underscore only
                if (!rawAction.matches("^[A-Za-z0-9_-]{1,50}$")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter.");
                    return;
                }

                // Build output safely (escape any dynamic content)
                StringBuilder html = new StringBuilder(512);
                html.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Settings</title></head><body>");
                html.append("<h1>").append(escapeHtml("Secret settings page!")).append("</h1>");
                html.append("<p>Requested action: ").append(escapeHtml(rawAction)).append("</p>");
                html.append("</body></html>");

                try (PrintWriter out = resp.getWriter()) {
                    out.write(html.toString());
                    out.flush();
                }

            } catch (Exception e) {
                // Avoid exposing internal details; log minimal information server-side.
                System.err.println("SettingsServlet error: " + e.getClass().getSimpleName());
                try {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
                } catch (IOException ignore) {
                    // Nothing we can do if sending the error fails
                }
            }
        }
    }
}
