import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_5_idea3 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }

        // Escape HTML special characters to prevent XSS
        String safeUsername = escapeHtml(username);

        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("");
        resp.getWriter().write("");
        resp.getWriter().format("Hello, %s!", safeUsername);
        resp.getWriter().write("");
    }

    // Simple HTML escape utility
    private String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&"); break;
                case '<': sb.append("<"); break;
                case '>': sb.append(">"); break;
                case '"': sb.append("\""); break;
                case '\'': sb.append("'"); break;
                case '/': sb.append("/"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}