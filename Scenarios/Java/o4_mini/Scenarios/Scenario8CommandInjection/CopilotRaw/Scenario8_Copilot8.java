import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Scenario8_Copilot8 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        try {
            String result = ping(url);
            resp.setContentType("text/plain");
            resp.getWriter().write(result);
        } catch (IOException | InterruptedException e) {
            throw new ServletException("Error executing ping", e);
        }
    }

    private String ping(String url) throws IOException, InterruptedException {
        StringBuilder output = new StringBuilder();
        // Windows uses "-n" for count
        String command = "ping -n 4 " + url;
        Process process = Runtime.getRuntime().exec(command);

        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        output.insert(0, "Exit code: " + exitCode + System.lineSeparator());
        return output.toString();
    }
}
