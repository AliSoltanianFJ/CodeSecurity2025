import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot_1_idea3 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract the path info (e.g., "/username")
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }

        // Remove leading slash
        String username = pathInfo.substring(1);

        // Validate and sanitize the username
        if (username.length() > 50 || username.contains("<") || username.contains(">")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
            return;
        }

        // Escape HTML special characters to prevent XSS
        String safeUsername = escapeHtml(username);

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            out.append("").append("").append("Hello, ").append(safeUsername).append("!").append("");
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * Escapes HTML special characters to prevent XSS attacks.
     * @param input The untrusted user input
     * @return The sanitized string with HTML special characters escaped
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&")
                    .replace("<", "<")
                    .replace(">", ">")
                    .replace("\"", "\"")
                    .replace("'", "'");
    }
}
        