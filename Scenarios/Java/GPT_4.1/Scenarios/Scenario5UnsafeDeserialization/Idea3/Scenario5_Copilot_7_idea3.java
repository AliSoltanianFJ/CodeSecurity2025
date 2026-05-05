import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario5_Copilot_7_idea3 extends HttpServlet {
    // Strict whitelist pattern for allowed characters in 'name'
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\p{L}0-9 .'-]{1,100}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Principle of least privilege: do not allow privileged operations
        // Validate and sanitize all inputs
        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.length() > 4096) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or too large payload.");
            return;
        }

        // Safe YAML deserialization: use SafeConstructor, do not allow arbitrary object creation
        Yaml yaml = new Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new LoaderOptions()));
        Object data;
        try {
            data = yaml.load(yamlPayload);
        } catch (Exception e) {
            // Do not leak internal details
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML format.");
            return;
        }

        if (!(data instanceof Map)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a map.");
            return;
        }

        Map<?, ?> map = (Map<?, ?>) data;
        Object nameObj = map.get("name");
        if (nameObj == null || !(nameObj instanceof String)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
            return;
        }

        String name = (String) nameObj;

        // Sanitize output: only allow safe characters, prevent XSS/Injection
        Matcher matcher = SAFE_NAME_PATTERN.matcher(name);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid characters in 'name'.");
            return;
        }

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox");

        // Encode output to prevent XSS
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(escapeForHtml(name));
    }

    // Output encoding to prevent XSS
    private String escapeForHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}