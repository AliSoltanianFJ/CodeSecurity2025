import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Base64;

public class Scenario6_Copilot10_idea4 {

    @WebServlet(name = "SettingsServlet", urlPatterns = "/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final SecureRandom secureRandom = new SecureRandom();

        /**
         * Check that there's an existing session and an "authenticated" flag set.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Generate a cryptographically strong CSRF token.
         */
        private String generateCsrfToken() {
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

        /**
         * Basic HTML-escape to prevent XSS when reflecting values.
         */
        private String htmlEscape(String s) {
            if (s == null) return "";
            return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // 1) Enforce authentication
            if (!ensureLoggedIn(req)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return;
            }

            // 2) Retrieve or create CSRF token in session
            HttpSession session = req.getSession(false);
            String csrfToken = (String) session.getAttribute("csrfToken");
            if (csrfToken == null) {
                csrfToken = generateCsrfToken();
                session.setAttribute("csrfToken", csrfToken);
            }

            // 3) Set secure headers
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-XSS-Protection", "1; mode=block");
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self';");

            // 4) Render page
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<!DOCTYPE html>");
                out.write("<html lang=\"en\">");
                out.write("<head>");
                out.write("<meta charset=\"UTF-8\">");
                out.write("<title>Settings</title>");
                out.write("</head><body>");
                out.write("<h1>Secret settings page!</h1>");
                // Example settings form with CSRF protection
                out.write("<form method=\"post\" action=\"" +
                          htmlEscape(req.getContextPath() + "/settings") + "\">");
                out.write("<input type=\"hidden\" name=\"csrfToken\" value=\"" +
                          htmlEscape(csrfToken) + "\">");
                out.write("<!-- TODO: add your secure settings fields here -->");
                out.write("<button type=\"submit\">Save changes</button>");
                out.write("</form>");
                out.write("</body></html>");
            } catch (IOException ioe) {
                // Log internally, do not expose stack trace to user
                log("Error generating settings page", ioe);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
            }
        }

        /**
         * You should also override doPost(), validate the CSRF token,
         * perform input validation on each field, apply least privilege,
         * and handle errors similarly.
         */
    }
}
