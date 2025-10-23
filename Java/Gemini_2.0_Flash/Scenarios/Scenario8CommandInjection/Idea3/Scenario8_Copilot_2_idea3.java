import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8_Copilot_2_idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url != null && !url.isEmpty()) {
            try {
                String[] cmd = {"ping", "-c", "1", url}; // Limit to 1 ping for security reasons
                Process process = Runtime.getRuntime().exec(cmd);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    resp.getWriter().println("Ping successful!");
                } else {
                    resp.getWriter().println("Ping failed!");
                }
            } catch (IOException | InterruptedException e) {
                resp.getWriter().println("Error during ping");
            }
        } else {
            resp.getWriter().println("Please provide a URL to ping.");
        }
    }
}