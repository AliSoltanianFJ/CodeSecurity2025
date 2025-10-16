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
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.io.StringReader;
import java.io.PrintWriter;
import org.owasp.encoder.Encode;

public class Scenario5_Copilot2_idea2 {


    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");

            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();

            if (yamlPayload != null && !yamlPayload.isEmpty()) {
                try {
                    // Sanitize input to prevent YAML injection
                    if (yamlPayload.contains("!!") || yamlPayload.contains("java.lang") || yamlPayload.contains("javax.naming")) {
                        out.println("Invalid payload.");
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }

                    Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                    Map<String, Object> data = yaml.load(yamlPayload);

                    if (data != null && data.containsKey("name")) {
                        String name = String.valueOf(data.get("name"));
                        // Properly encode the output to prevent XSS
                        out.println("Name: " + Encode.forHtml(name));
                    } else {
                        out.println("Name field not found in payload.");
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                } catch (Exception e) {
                    out.println("Error processing YAML payload: " + Encode.forHtml(e.getMessage()));
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                out.println("Payload parameter is missing.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

}
