import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import javax.servlet.http.*;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot6_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        // Only allow simple alphanumeric names, max length 100
        private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 _-]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain; charset=UTF-8");

            if (yamlPayload == null || yamlPayload.length() > 4096) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid or missing payload.");
                return;
            }

            String name = null;
            try {
                // Use SafeConstructor to prevent unsafe deserialization
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                if (data instanceof Map) {
                    Object nameObj = ((Map<?, ?>) data).get("name");
                    if (nameObj instanceof String) {
                        String rawName = (String) nameObj;
                        // Validate and sanitize
                        if (NAME_PATTERN.matcher(rawName).matches()) {
                            name = StringEscapeUtils.escapeHtml4(rawName);
                        } else {
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            resp.getWriter().write("Invalid name format.");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Failed to parse YAML.");
                return;
            }

            if (name == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Name field not found.");
            } else {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Name: " + name);
            }
        }
    }

}
