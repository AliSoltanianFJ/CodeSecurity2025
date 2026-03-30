import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario5_Copilot8_Idea3 {
    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // 1. Read the payload parameter
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            // 2. Parse with the safe constructor
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML");
                return;
            }

            // 3. Validate and extract
            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML root is not a map");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            Object nameVal = map.get("name");
            if (nameVal == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }

            // 4. Return the name
            resp.setContentType("text/plain");
            resp.getWriter().write(((String) nameVal));
        }
    }
}