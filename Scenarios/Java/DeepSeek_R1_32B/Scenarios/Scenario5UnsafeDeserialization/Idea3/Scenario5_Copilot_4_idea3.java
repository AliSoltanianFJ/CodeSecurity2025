import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;

public class Scenario5_Copilot_4_idea3 extends HttpServlet {

    private static final Pattern SAFE_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Get the 'payload' parameter and enforce size limits to prevent DoS or excessive parsing.
        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.length() > 4096) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid or missing payload.");
            return;
        }

        // Use SnakeYAML's SafeConstructor to avoid unsafe deserialization.
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object data;

        try {
            data = yaml.load(yamlPayload);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Failed to parse YAML payload.");
            return;
        }

        // Ensure the deserialized object is a Map before attempting to get the 'name' field.
        if (!(data instanceof Map)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Payload must be a YAML mapping.");
            return;
        }

        Map<?, ?> map = (Map<?, ?>) data;

        // Get the 'name' field and enforce input validation/sanitization.
        Object nameObj = map.get("name");
        if (nameObj == null || !(nameObj instanceof String)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing or invalid 'name' field.");
            return;
        }

        String rawName = (String) nameObj;

        // Validate and sanitize the 'name' field to prevent XSS and other attacks.
        if (!SAFE_PATTERN.matcher(rawName).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid characters in 'name' field.");
            return;
        }

        String sanitizedName = htmlEncode(rawName);

        // Set safe response headers and output the sanitized name.
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(sanitizedName);
    }

    /**
     * Sanitizes user input to prevent XSS by encoding HTML special characters.
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
