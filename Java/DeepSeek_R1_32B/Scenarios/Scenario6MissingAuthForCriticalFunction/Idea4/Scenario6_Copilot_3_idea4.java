import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;
import java.io.PrintWriter;
import java.util.Optional;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario6_Copilot_3_idea4 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized access");
                    return;
                }

                // Set security headers
                resp.setHeader("Content-Security-Policy", "default-src 'self';");
                resp.setHeader("X-Content-Type-Options", "nosniff");

                resp.setContentType("text/html; charset=UTF-8");
                PrintWriter writer = resp.getWriter();
                
                // Properly escape output (if dynamic data is used)
                String content = "<html><h1>Secret settings page!</h1></html>";
                writer.write(content);
            } catch (Exception e) {
                // Log the error securely
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
            } finally {
                if (resp.getWriter() != null) {
                    resp.getWriter().close();
                }
            }
        }
    }

}
