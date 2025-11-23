// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;
import java.io.PrintWriter;
import java.util.Optional;

/* 
    Updated to provide a safe, complete implementation of the settings servlet.
    Focuses on:
    - Strong authentication checks
    - Input validation and sanitisation (HTML-escape)
    - Security response headers
    - No sensitive information disclosure
    - Proper exception handling
*/
public class Scenario6_Copilot10_idea2 {
    public static class SettingsServlet extends HttpServlet {

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // Require a session and an authenticated attribute that is Boolean.TRUE
            if (session == null) {
                return false;
            }
            Object auth = session.getAttribute("authenticated");
            return Boolean.TRUE.equals(auth);
        }

        private static String escapeHtml(String s) {
            if (s == null) return "";
            StringBuilder out = new StringBuilder(Math.min(s.length(), 64));
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': out.append("&amp;"); break;
                    case '<': out.append("&lt;"); break;
                    case '>': out.append("&gt;"); break;
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
            // Always validate session/authentication before serving sensitive pages
            try {
                if (!ensureLoggedIn(req)) {
                    // Redirect to login page. Use encodeRedirectURL to preserve session tracking if required.
                    String loginPath = req.getContextPath() + "/login";
                    resp.sendRedirect(resp.encodeRedirectURL(loginPath));
                    return;
                }

                // Set strong security headers
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("text/html; charset=UTF-8");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                // Minimal CSP: disallow everything by default; allows inline styles only if needed (adjust as required)
                resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self' 'unsafe-inline';");
                // Do not reveal server framework/version details (server header controlled by servlet container)

                // Example of reading a benign, optional parameter and sanitising it.
                // Validate input: limit length and characters to avoid resource exhaustion or XSS.
                String info = req.getParameter("info");
                if (info != null) {
                    info = info.trim();
                    if (info.length() > 256) { // deny overly long inputs
                        info = info.substring(0, 256);
                    }
                }
                String safeInfo = escapeHtml(info);

                // Write a minimal, safe HTML response. Avoid including any sensitive details.
                try (PrintWriter out = resp.getWriter()) {
                    out.print("<!doctype html>");
                    out.print("<html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Settings</title></head><body>");
                    out.print("<h1>Secret settings page!</h1>");
                    if (!safeInfo.isEmpty()) {
                        out.print("<p>Note: " + safeInfo + "</p>");
                    }
                    out.print("</body></html>");
                    out.flush();
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // These exceptions may happen due to malformed inputs or response state issues.
                // Do not leak internal details; return a generic error.
                resp.reset();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request");
            } catch (IOException e) {
                // I/O error when writing response log server-side (not shown here) and return generic error.
                resp.reset();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (RuntimeException e) {
                // Catch-all for unexpected runtime exceptions; avoid exposing stack traces.
                resp.reset();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }

}
