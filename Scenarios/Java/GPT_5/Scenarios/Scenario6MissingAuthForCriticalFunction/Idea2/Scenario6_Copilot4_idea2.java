// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario6_Copilot4_idea2 {
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger LOGGER = Logger.getLogger(SettingsServlet.class.getName());
        private static final int MAX_USERNAME_LENGTH = 64;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        private static String escapeHtml(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length());
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '&': sb.append("&amp;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#x27;"); break;
                    case '/': sb.append("&#x2F;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Security headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Referrer-Policy", "no-referrer");
            resp.setHeader("Content-Security-Policy", "default-src 'self'");

            // Use UTF-8
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html; charset=UTF-8");

            try {
                // Ensure authenticated
                if (!ensureLoggedIn(req)) {
                    // Don't leak any internal info, just require authentication
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    return;
                }

                HttpSession session = req.getSession(false);
                if (session == null) { // defensive: should not happen after ensureLoggedIn
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    return;
                }

                // Authorization check: require explicit role (reduce privilege creep)
                Object roleObj = session.getAttribute("role");
                if (!(roleObj instanceof String) || !"admin".equals(roleObj)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient privileges");
                    return;
                }

                // Retrieve and validate username display (do not expose sensitive data)
                String rawUser = "";
                Object unameObj = session.getAttribute("username");
                if (unameObj instanceof String) {
                    rawUser = (String) unameObj;
                    if (rawUser.length() > MAX_USERNAME_LENGTH) {
                        rawUser = rawUser.substring(0, MAX_USERNAME_LENGTH);
                    }
                }

                String safeUser = escapeHtml(rawUser);

                // Avoid reflecting any request parameters without validation and encoding
                try (PrintWriter out = resp.getWriter()) {
                    // Minimal, safe HTML output; no private information exposed
                    out.print("<!doctype html><html><head><meta charset=\"UTF-8\"><title>Settings</title></head><body>");
                    out.print("<h1>Secret settings page</h1>");
                    if (!safeUser.isEmpty()) {
                        out.print("<p>Signed in as: " + safeUser + "</p>");
                    }
                    out.print("<p>No sensitive details are shown here.</p>");
                    out.print("</body></html>");
                    out.flush();
                }
            } catch (IllegalArgumentException iae) {
                // Input validation problems
                LOGGER.log(Level.WARNING, "Invalid input to settings servlet", iae);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            } catch (Exception e) {
                // Generic error handling: log server-side, do not leak stack traces to client
                LOGGER.log(Level.SEVERE, "Unexpected error in SettingsServlet", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
// ...existing code...
