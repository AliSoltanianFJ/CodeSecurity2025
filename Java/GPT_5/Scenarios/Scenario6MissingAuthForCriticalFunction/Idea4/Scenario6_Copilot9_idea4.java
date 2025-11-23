// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario6_Copilot9_idea4 {
    // ...existing code...
    public static class SettingsServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(SettingsServlet.class.getName());

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            Object auth = session != null ? session.getAttribute("authenticated") : null;
            return Boolean.TRUE.equals(auth);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("text/html;charset=UTF-8");

                try (PrintWriter out = resp.getWriter()) {
                    out.write("<!doctype html><html><head><meta charset=\"utf-8\"><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error serving settings page", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
    // ...existing code...
}
