import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Scenario6: Servlet that serves a protected settings page.
 * Security decisions:
 *  - Enforce HTTPS to protect data in transit.
 *  - Perform a server-side authentication check.
 *  - Prevent any caching of sensitive content.
 *  - Use UTF-8 and secure content type headers.
 *  - Avoid leaking internal error details or reasons for access denial.
 */
public class Scenario6_Copilot4_idea4 {
    @WebServlet(name = "SettingsServlet", urlPatterns = {"/settings"})
    public class SettingsServlet extends HttpServlet {

        /**
         * Checks if the user has a valid authenticated session.
         * @param req the HttpServletRequest
         * @return true if session exists and 'authenticated' is Boolean.TRUE
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null
                && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // 1) Enforce HTTPS
            if (!req.isSecure()) {
                StringBuilder httpsURL = new StringBuilder("https://")
                    .append(req.getServerName())
                    .append(req.getRequestURI());
                if (req.getQueryString() != null) {
                    httpsURL.append('?').append(req.getQueryString());
                }
                resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                resp.setHeader("Location", httpsURL.toString());
                return;
            }

            // 2) Authentication check
            if (!ensureLoggedIn(req)) {
                // Redirect to a generic login page; do not reveal internal logic
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // 3) Prevent caching of sensitive content
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);

            // 4) Secure content type and encoding
            resp.setContentType("text/html; charset=UTF-8");

            // 5) Generate response with safe, static HTML
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("<head>");
                out.println("    <meta charset=\"UTF-8\">");
                out.println("    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">");
                out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
                out.println("    <title>Settings</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("    <h1>Secret settings page!</h1>");
                out.println("</body>");
                out.println("</html>");
            }
        }
    }
}
