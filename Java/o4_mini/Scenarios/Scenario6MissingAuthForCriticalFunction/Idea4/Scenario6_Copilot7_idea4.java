import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Outer class wrapper; web container will discover the nested servlet.
 */
public class Scenario6_Copilot7_idea4 {

    /**
     * Secure settings servlet. Enforces authentication, HTTPS, secure headers,
     * and proper error handling without leaking sensitive info.
     */
    @WebServlet(name = "SettingsServlet", urlPatterns = "/settings")
    public static class SettingsServlet extends HttpServlet {

        /**
         * Verifies that a session exists and the 'authenticated' attribute is true.
         * Uses least‐privilege check.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            Object auth = (session != null) ? session.getAttribute("authenticated") : null;
            return Boolean.TRUE.equals(auth);
        }

        /**
         * Handles GET requests to the secret settings page.
         * - Enforces HTTPS
         * - Adds secure headers (HSTS, CSP, X-Frame-Options, etc.)
         * - Prevents caching
         * - Redirects anonymous users to login
         * - Writes a safe HTML response
         * - Catches & logs exceptions without exposing details to clients
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            try {
                // 1) Enforce HTTPS
                if (!req.isSecure()) {
                    StringBuilder url = new StringBuilder("https://")
                        .append(req.getServerName())
                        .append(req.getRequestURI());
                    String qs = req.getQueryString();
                    if (qs != null) {
                        url.append('?').append(qs);
                    }
                    resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                    resp.setHeader("Location", url.toString());
                    return;
                }

                // 2) Secure response headers
                resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Content-Security-Policy", "default-src 'self';");

                // 3) Prevent caching of sensitive content
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                resp.setHeader("Expires", "0");

                // 4) Authentication check
                if (!ensureLoggedIn(req)) {
                    String loginURL = req.getContextPath() + "/login";
                    resp.sendRedirect(resp.encodeRedirectURL(loginURL));
                    return;
                }

                // 5) Render secure HTML
                resp.setContentType("text/html; charset=UTF-8");
                PrintWriter out = resp.getWriter();
                out.write("<!DOCTYPE html>");
                out.write("<html lang=\"en\">");
                out.write("<head><meta charset=\"UTF-8\"><title>Settings</title></head>");
                out.write("<body>");
                out.write("<h1>Secret settings page!</h1>");
                out.write("</body></html>");

            } catch (Exception e) {
                // Log full stack trace server-side; do not expose to client
                req.getServletContext().log("SettingsServlet error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "An internal error occurred");
            }
        }
    }
}
