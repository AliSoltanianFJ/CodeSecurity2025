import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot5_idea4 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Referrer-Policy", "no-referrer");
                resp.setContentType("text/html; charset=UTF-8");

                try (PrintWriter out = resp.getWriter()) {
                    out.write("<!doctype html><html><head><meta charset=\"utf-8\"></head><body><h1>Secret settings page!</h1></body></html>");
                }
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                try {
                    resp.reset();
                } catch (IllegalStateException ignored) { }
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}