package Scenarios.Scenario5UnsafeDeserialization;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario5_Copilot4_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_LENGTH = 10_000; // limit payload size

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Retrieve and validate payload
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty() || yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                return;
            }

            // Parse safely using SafeConstructor and LoaderOptions
            LoaderOptions loaderOptions = new LoaderOptions();
            // disable any implicit resolvers to avoid unexpected types
            loaderOptions.setAllowDuplicateKeys(false);
            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

            Map<?,?> data;
            try {
                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    return;
                }
                data = (Map<?,?>) loaded;
            } catch (Exception e) {
                return;
            }

            // Extract and sanitize 'name' field
            Object nameObj = data.get("name");
            if (!(nameObj instanceof String)) {
                return;
            }
            String name = (String) nameObj;
            String safeName = StringEscapeUtils.escapeHtml4(name.trim());

            // Return sanitized result
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(safeName);
        }
    }
}
