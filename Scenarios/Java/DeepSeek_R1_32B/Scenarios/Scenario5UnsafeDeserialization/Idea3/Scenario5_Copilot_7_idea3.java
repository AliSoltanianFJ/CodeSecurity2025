import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.error.YAMLException;
import javax.servlet.ServletException;

public class Scenario5_Copilot_7_idea3 extends HttpServlet {

    // Allow only alphanumeric characters, underscore, and hyphen in 'name' field
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\w-]{1,100}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Ensure response uses secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentType("text/plain; charset=UTF-8");

        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
            // Do not leak internal details
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing or empty payload.");
            return;
        }

        String nameValue = null;

        try {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object data = yaml.load(yamlPayload);

            if (!(data instanceof Map)) {
                // Do not expose internal errors
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid YAML structure.");
                return;
            }

            Map<?, ?> map = (Map<?, ?>) data;
            Object nameObj = map.get("name");

            if (nameObj == null || !(nameObj instanceof String)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("'name' field missing or not a string.");
                return;
            }

            String rawName = (String) nameObj;

            // Validate and sanitize the 'name' value
            if (!NAME_PATTERN.matcher(rawName).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid 'name' value.");
                return;
            }

            // Basic HTML encoding to prevent XSS in case of future output
            String safeName = htmlEncode(rawName);

            // Return the sanitized 'name'
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(safeName);

        } catch (YAMLException e) {
            // Log error securely (not shown here), do not leak details to client
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid YAML format.");
        } catch (IOException e) {
            // Handle internal errors, do not expose stack traces
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal error.");
        }
    }

    /**
     * Basic HTML encoding to prevent XSS.
     */
    private static String htmlEncode(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

}
