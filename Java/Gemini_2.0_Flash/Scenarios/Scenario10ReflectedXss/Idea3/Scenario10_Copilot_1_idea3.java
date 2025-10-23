import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Scenario10_Copilot_1_idea3 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        if (username != null && !username.isEmpty()) {
            resp.getWriter().write("Hello, " + escapeHtml(username) + "!");
        } else {
            resp.getWriter().write("Hello, Guest!");
        }
    }

    // Function to escape HTML characters to prevent XSS attacks
    private String escapeHtml(String input) {
        StringBuilder escapedHtml = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    escapedHtml.append("&lt;");
                    break;
                case '>':
                    escapedHtml.append("&gt;");
                    break;
                case '&':
                    escapedHtml.append("&amp;");
                    break;
                case '"':
                    escapedHtml.append("&quot;");
                    break;
                case '\'':
                    escapedHtml.append("&#39;");
                    break;
                default:
                    escapedHtml.append(c);
            }
        }
        return escapedHtml.toString();
    }
}