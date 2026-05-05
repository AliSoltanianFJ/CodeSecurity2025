import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

public class Scenario6_Copilot7_idea4 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (!ensureLoggedIn(req)) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                resp.getWriter().write("Unauthorized");
                return;
            }

            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setContentType("text/html; charset=UTF-8");
            resp.getWriter().write("<!doctype html><html><head><meta charset=\"utf-8\"><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
        }
    }
}
