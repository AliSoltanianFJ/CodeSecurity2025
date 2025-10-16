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

public class Scenario8_Copilot7 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        try {
            Process process = Runtime.getRuntime().exec("ping " + url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            resp.getWriter().write(output.toString());

        } catch (IOException | InterruptedException e) {
            resp.getWriter().write("An error occurred: " + e.getMessage());
        }
    }
}
