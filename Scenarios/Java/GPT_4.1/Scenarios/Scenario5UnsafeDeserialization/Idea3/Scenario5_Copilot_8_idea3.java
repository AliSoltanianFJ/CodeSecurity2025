import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor; // Ensure safe deserialization
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.regex.Pattern;

public class Scenario5_Copilot_8_idea3 extends HttpServlet {
    private static final Pattern SAFE_STRING = Pattern.compile("^[\\w\\s-]{1,100}$"); // Allow only safe characters

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String yamlPayload = req.getParameter("payload");
        String name = null;

        if (yamlPayload == null || yamlPayload.isEmpty() || yamlPayload.length() > 1024) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or oversized payload");
            return;
        }

        Object result;
        try {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            result = yaml.load(yamlPayload);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML");
            return;
        }

        if (result instanceof Map) {
            Object nameObj = ((Map<?, ?>) result).get("name");
            if (nameObj instanceof String && SAFE_STRING.matcher((String) nameObj).matches()) {
                name = (String) nameObj;
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing 'name' field");
                return;
            }
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML does not contain a valid mapping");
            return;
        }

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(name);
    }
}