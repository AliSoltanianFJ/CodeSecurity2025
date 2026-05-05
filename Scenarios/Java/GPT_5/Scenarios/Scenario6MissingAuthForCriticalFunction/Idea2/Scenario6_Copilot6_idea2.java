package scenarios; // optional - adjust or remove to match your project layout

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Container class for the servlet used in the exercise.
 * The nested servlet is made static so it can be instantiated by the servlet container.
 * This file is written to be safe: validates session authentication, sets secure response headers,
 * avoids reflecting user input, and handles errors without revealing sensitive details.
 */
public class Scenario6_Copilot6_idea2 {

    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final int INITIAL_HTML_CAPACITY = 512;

        /**
         * Stronger check for an authenticated session.
         * Only returns true when the session exists and the "authenticated" attribute equals Boolean.TRUE.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Serve the settings page only for authenticated users.
         * Uses safe fixed redirects for unauthenticated requests, defensive headers to mitigate XSS/clickjacking,
         * and avoids reflecting any user-controlled data into the response.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    // Do not reveal internals or accept redirect targets from user input (prevents open redirect).
                    String loginPath = req.getContextPath() + "/login";
                    // Use 302/303 redirect for interactive clients; mark as unauthorized for clarity.
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                    resp.sendRedirect(loginPath);
                    return;
                }

                // Security response headers to mitigate XSS, MIME sniffing and clickjacking
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("text/html; charset=UTF-8");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none'; frame-ancestors 'none';");
                resp.setHeader("Referrer-Policy", "no-referrer");
                resp.setHeader("Permissions-Policy", "geolocation=(), microphone=()");

                // Build HTML safely; do not include any unsanitized user input
                StringBuilder html = new StringBuilder(INITIAL_HTML_CAPACITY);
                html.append("<!doctype html>");
                html.append("<html lang=\"en\">");
                html.append("<head>");
                html.append("<meta charset=\"UTF-8\">");
                html.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">");
                html.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
                html.append("<title>Settings</title>");
                html.append("</head>");
                html.append("<body>");
                html.append("<h1>Secret settings page!</h1>");
                html.append("<p>Settings are available to authenticated users only.</p>");
                html.append("</body>");
                html.append("</html>");

                try (PrintWriter out = resp.getWriter()) {
                    out.write(html.toString());
                    out.flush();
                }
            } catch (IllegalStateException | IOException ex) {
                // Fail closed: do not reveal stack traces or internal details to the client.
                // Loggers would normally be used here; omitted to avoid disclosing environment specifics.
                try {
                    resp.reset();
                } catch (IllegalStateException ise) {
                    // ignore - response already committed
                }
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
