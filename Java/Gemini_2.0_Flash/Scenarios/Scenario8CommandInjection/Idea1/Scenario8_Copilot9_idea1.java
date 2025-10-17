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

public class Scenario8_Copilot9_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        try {
            // Validate the URL to prevent command injection
            if (url == null || url.isEmpty() || !isValidURL(url)) {
                resp.getWriter().write("Invalid URL provided.");
                return;
            }

            ProcessBuilder processBuilder = new ProcessBuilder("ping", url);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                resp.getWriter().write(output.toString());
            } else {
                resp.getWriter().write("Ping command failed with exit code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            resp.getWriter().write("An error occurred: " + e.getMessage());
        }
    }

    private boolean isValidURL(String url) {
        // Implement a more robust URL validation logic here.
        // This is a basic example and might need to be improved.
        return url.startsWith("http://") || url.startsWith("https://") || url.matches("^[a-zA-Z0-9.-]+$");
    }
}
