import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot9_idea1 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        try {
            if (username == null || username.isEmpty()) {
                out.println("Username parameter is missing.");
                return;
            }

            String safeName = escapeHtml(username);

            resp.setContentType("text/html;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
                out.println("<h1>Hello, " + safeName + "!</h1>");
                out.println("</body></html>");
            }
        } catch (Exception e) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("An internal error occurred.");
            } catch (IOException ioe) {}
        }
    }

    // Escapes HTML special characters to prevent XSS
    private String escapeHtml(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '>': sb.append("&gt;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
