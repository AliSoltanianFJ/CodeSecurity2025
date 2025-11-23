import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario6_Copilot2_idea2 {
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger LOG = Logger.getLogger(SettingsServlet.class.getName());

        /**
         * Stronger check for logged-in state: accepts Boolean.TRUE or String "true" (case-insensitive).
         * Avoids trusting arbitrary non-null session attributes.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            if (session == null) {
                return false;
            }
            Object auth = session.getAttribute("authenticated");
            if (auth instanceof Boolean) {
                return Boolean.TRUE.equals(auth);
            }
            if (auth instanceof String) {
                return "true".equalsIgnoreCase((String) auth);
            }
            return false;
        }

        private static String escapeHtml(String s, int maxLen) {
            if (s == null) return "";
            if (s.length() > maxLen) s = s.substring(0, maxLen);
            StringBuilder out = new StringBuilder(s.length());
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '&': out.append("&amp;"); break;
                    case '<': out.append("&lt;"); break;
                    case '>': out.append("&gt;"); break;
                    case '\"': out.append("&quot;"); break;
                    case '\'': out.append("&#x27;"); break;
                    case '/': out.append("&#x2F;"); break;
                    default: out.append(c);
                }
            }
            return out.toString();
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Validate and enforce authentication
                if (!ensureLoggedIn(req)) {
                    // Invalidate any unauthenticated session to reduce risk (session fixation)
                    HttpSession s = req.getSession(false);
                    if (s != null) {
                        try {
                            s.removeAttribute("authenticated");
                            s.invalidate();
                        } catch (IllegalStateException ignored) {
                            // session already invalidated; ignore
                        }
                    }
                    // Redirect to a login page (use 303/SEE_OTHER to prevent resubmission)
                    String loginPath = req.getContextPath() + "/login";
                    resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    resp.setHeader("Location", loginPath);
                    return;
                }

                // Security headers to mitigate XSS and content sniffing
                resp.setContentType("text/html; charset=UTF-8");
                resp.setCharacterEncoding("UTF-8");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Referrer-Policy", "no-referrer");
                // Strict CSP: no inline scripts/styles allowed, only allow same-origin for safe assets
                resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self'; img-src 'self'; font-src 'self';");

                // Minimal safe HTML output. Avoid reflecting any user-controlled input.
                try (PrintWriter out = resp.getWriter()) {
                    out.println("<!doctype html>");
                    out.println("<html lang=\"en\">");
                    out.println("<head>");
                    out.println("  <meta charset=\"utf-8\"/>");
                    out.println("  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/>");
                    out.println("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>");
                    out.println("  <title>Settings</title>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("  <h1>Secret settings page</h1>");
                    out.println("  <p>Your settings are protected and not displayed here.</p>");
                    out.println("</body>");
                    out.println("</html>");
                    out.flush();
                }
            } catch (Exception e) {
                // Log detailed error server-side, but do not leak details to the client
                LOG.log(Level.SEVERE, "Error handling settings request", e);
                try {
                    resp.reset();
                } catch (IllegalStateException ignored) {
                    // response already committed; nothing to do
                }
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write("Internal server error");
            }
        }
    }

}
