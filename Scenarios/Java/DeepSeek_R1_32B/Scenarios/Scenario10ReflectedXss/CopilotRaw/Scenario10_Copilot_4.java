import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;

public class Scenario10_Copilot_4 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String message;

        if (username == null || username.isEmpty()) {
            // If no username is provided, default to "Guest"
            message = "Hello, Guest!";
        } else {
            // Create a personalized greeting
            message = "Hello, " + username + "!";
        }

        resp.setStatus(200);
        try (PrintWriter out = resp.getWriter()) {
            out.println(message);
        }
    }
}

