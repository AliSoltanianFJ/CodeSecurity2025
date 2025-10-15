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

public class Scenario8_Copilot9 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        String result = ping(url);
        resp.setContentType("text/plain");
        resp.getWriter().write(result);
    }

    private String ping(String url) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        BufferedReader reader = null;
        try {
            // Only allow safe characters in the URL (basic validation)
            if (url == null || !url.matches("^[a-zA-Z0-9.\\-]+$")) {
                return "Invalid URL format.";
            }
            // Windows uses "-n", Linux uses "-c"
            String command = "ping -n 1 " + url;
            process = Runtime.getRuntime().exec(command);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            int exitCode = process.waitFor();
            output.append("Exit code: ").append(exitCode);
        } catch (IOException | InterruptedException e) {
            output.append("Error: ").append(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    output.append("Error closing reader: ").append(e.getMessage());
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        return output.toString();
    }
}
