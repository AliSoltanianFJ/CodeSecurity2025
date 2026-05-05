// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot10 {
    // Made static so servlet container can instantiate this class
    public static class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Ensure only authenticated users can access this critical function
            if (!ensureLoggedIn(req)) {
                // Redirect unauthenticated users to the login page
                String loginUrl = req.getContextPath() + "/login";
                try {
                    resp.sendRedirect(loginUrl);
                } catch (IOException e) {
                    // If redirect fails, send a 401/302 fallback
                    try {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    } catch (IOException ignored) {
                        // Nothing we can do if sending the error also fails
                    }
                }
                return;
            }

            // Authenticated: render the settings page
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<!DOCTYPE html>");
                out.write("<html>");
                out.write("<head><meta charset=\"utf-8\"><title>Settings</title></head>");
                out.write("<body>");
                out.write("<h1>Secret settings page!</h1>");
                out.write("</body>");
                out.write("</html>");
            } catch (IOException e) {
                // If writing the response fails, return an internal server error
                try {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to render settings page");
                } catch (IOException ignored) {
                    // ignore secondary failure
                }
            }
        }
    }
}
// ...existing code...
