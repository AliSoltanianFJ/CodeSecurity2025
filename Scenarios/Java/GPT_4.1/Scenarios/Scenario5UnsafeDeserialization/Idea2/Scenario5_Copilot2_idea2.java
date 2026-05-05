import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import javax.servlet.http.*;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.text.StringEscapeUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot2_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        // Only allow simple alphanumeric names, max 100 chars
        private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 _-]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Validate input: check for null and reasonable length
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.length() > 2048) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload.");
                return;
            }

            // Prevent insecure deserialization: only parse to Map, not to arbitrary objects
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
            Map<String, Object> data;
            try {
                Object parsed = yaml.load(yamlPayload);
                if (!(parsed instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload must be a YAML map.");
                    return;
                }
                data = (Map<String, Object>) parsed;
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse YAML.");
                return;
            }

            // Validate and sanitize 'name' field
            Object nameObj = data.get("name");
            if (nameObj == null || !(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
                return;
            }
            String name = (String) nameObj;

            // Validate against pattern
            Matcher matcher = NAME_PATTERN.matcher(name);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid name format.");
                return;
            }

            // Prevent XSS: escape output
            String safeName = StringEscapeUtils.escapeHtml4(name);

            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(safeName);
        }
    }

}
