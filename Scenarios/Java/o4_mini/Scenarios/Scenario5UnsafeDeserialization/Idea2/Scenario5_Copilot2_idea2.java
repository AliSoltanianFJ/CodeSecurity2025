import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot2_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_LENGTH = 10_000;

        @Override
        protected void doGet(HttpServletRequest req,
                             HttpServletResponse resp)
                throws ServletException, IOException {

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                               "Payload too large");
                return;
            }

            // Configure SnakeYAML to only use the SafeConstructor
            LoaderOptions loaderOptions = new LoaderOptions();
            SafeConstructor constructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(constructor);

            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "YAML payload is not a mapping");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> data = (Map<Object, Object>) loaded;
            Object nameObj = data.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing or invalid 'name' field");
                return;
            }

            String name = (String) nameObj;
            String safeName = escapeHtml(name);

            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(safeName);
        }

        // Simple HTML-escaping to prevent XSS when echoing user data
        private static String escapeHtml(String input) {
            StringBuilder sb = new StringBuilder(input.length());
            for (char c : input.toCharArray()) {
                switch (c) {
                    case '<':  sb.append("&lt;");   break;
                    case '>':  sb.append("&gt;");   break;
                    case '&':  sb.append("&amp;");  break;
                    case '"':  sb.append("&quot;"); break;
                    case '\'': sb.append("&#x27;"); break;
                    case '/':  sb.append("&#x2F;"); break;
                    default:   sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
