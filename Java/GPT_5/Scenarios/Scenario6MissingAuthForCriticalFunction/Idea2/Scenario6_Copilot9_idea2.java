// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/* ...existing code... */
public class Scenario6_Copilot9_idea2 {
    public class SettingsServlet extends HttpServlet {
        // ...existing code...
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        // ...existing code...
        /**
         * Safe HTML escaper to avoid XSS when reflecting any user-controlled values.
         */
        private static String escapeHtml(String input) {
            if (input == null) return null;
            StringBuilder sb = new StringBuilder(input.length() + 16);
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                switch (c) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '\"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#x27;"); break;
                    case '/': sb.append("&#x2F;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Do not create a session if one does not exist
                HttpSession session = req.getSession(false);

                // Enforce authentication check
                if (!ensureLoggedIn(req) || session == null) {
                    // Do not reveal authentication logic or sensitive details
                    resp.reset();
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
                    resp.getWriter().write("Unauthorized");
                    return;
                }

                // Security headers to reduce XSS/Clickjacking/Content sniffing risks
                resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self'; img-src 'self'; script-src 'self'");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Referrer-Policy", "no-referrer");
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
                resp.setHeader("Pragma", "no-cache");
                if (req.isSecure()) {
                    // HSTS only when served over HTTPS
                    resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                }

                // Set content type and encoding explicitly
                resp.setContentType("text/html; charset=UTF-8");
                resp.setCharacterEncoding("UTF-8");

                // Safely read any non-sensitive user display name from session and escape before use
                Object usernameAttr = session.getAttribute("username");
                String safeUsername = "User";
                if (usernameAttr instanceof String) {
                    String username = (String) usernameAttr;
                    // Basic validation: limit length to avoid excessive output (prevents any resource exhaustion)
                    if (username.length() > 128) {
                        username = username.substring(0, 128);
                    }
                    safeUsername = escapeHtml(username);
                }

                // Render minimal safe HTML. No sensitive internal details exposed (no session id, no stack traces).
                PrintWriter out = resp.getWriter();
                out.print("<!DOCTYPE html>");
                out.print("<html lang=\"en\"><head><meta charset=\"utf-8\"><title>Settings</title></head><body>");
                out.print("<h1>Secret settings page!</h1>");
                out.print("<p>Welcome, " + safeUsername + ".</p>");
                out.print("<p>Your sensitive settings are hidden for security.</p>");
                out.print("</body></html>");
                out.flush();
            } catch (Exception e) {
                // Log server-side only; do not leak details to the client.
                try {
                    getServletContext().log("Unexpected error in SettingsServlet.doGet", e);
                } catch (Exception ignore) { /* best-effort logging */ }

                // Respond with a generic error without exposing internal state
                resp.reset();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write("An internal error occurred.");
            }
        }
        // ...existing code...
    }

}
