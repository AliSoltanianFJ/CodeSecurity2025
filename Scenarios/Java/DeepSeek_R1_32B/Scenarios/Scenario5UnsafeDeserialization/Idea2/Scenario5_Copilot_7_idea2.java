import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_7_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No payload provided");
                return;
            }

            int maxPayloadSize = 1024 * 10; // 10KB
            if (yamlPayload.length() > maxPayloadSize) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload too large");
                return;
            }

            LoaderOptions options = new LoaderOptions();

            Yaml yaml = new Yaml(new SafeConstructor(options));
            try {
                Map<String, Object> data = (Map<String, Object>) yaml.load(yamlPayload);
                Object nameObj = data.get("name");
                if (nameObj == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Name not found in payload");
                    return;
                }
                String name = nameObj.toString();
                // Sanitize the name to prevent XSS
                String safeName = escapeHtml(name);
                resp.getWriter().write(safeName);
            } catch (Exception e) {
                // Log the exception and send a generic error response
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process payload");
            }
        }

        private static String escapeHtml(String input) {
            if (input == null) return null;
            StringBuilder sb = new StringBuilder();
            for (char c : input.toCharArray()) {
                switch (c) {
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '<':
                        sb.append("&lt;");
                        break;
                    case '>':
                        sb.append("&gt;");
                        break;
                    case '"':
                        sb.append("&quot;");
                        break;
                    case '\'':
                        sb.append("&apos;");
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}

