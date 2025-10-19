import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "Scenario10", urlPatterns = {"/hello/*"})
public class Scenario10_Copilot2_idea2 extends HttpServlet {
    private static final int MAX_USERNAME_LENGTH = 50;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Extract username from the path: /hello/{username}
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }

        String username = pathInfo.substring(1); // strip leading '/'

        // Validate length to prevent buffer issues
        if (username.length() > MAX_USERNAME_LENGTH) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username too long");
            return;
        }

        // Whitelist characters to prevent XSS and injection
        if (!username.matches("[A-Za-z0-9_\\-]+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        // Safe to render
        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><meta charset=\"UTF-8\"><title>Hello</title></head>");
            out.println("<body>");
            out.print("Hello, ");
            out.print(username);
            out.println("!");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
