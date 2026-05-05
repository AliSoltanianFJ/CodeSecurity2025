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

public class Scenario5_Copilot9_idea4 {


    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            PrintWriter out = resp.getWriter();
            resp.setContentType("text/html");

            if (yamlPayload != null && !yamlPayload.isEmpty()) {
                try {
                    // Use SafeConstructor with LoaderOptions to prevent insecure deserialization
                    Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                    Map<String, Object> data = yaml.load(yamlPayload);

                    if (data != null && data.containsKey("name")) {
                        // Context-aware output encoding to prevent XSS
                        String name = Encode.forHtml(String.valueOf(data.get("name")));
                        out.println("Name: " + name);
                    } else {
                        out.println("Name field not found in YAML payload.");
                    }
                } catch (Exception e) {
                    // Log the error securely (do not expose sensitive information)
                    System.err.println("Error processing YAML payload: " + e.getMessage());
                    // Return a generic error message to the client
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("An error occurred while processing the request.");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Payload parameter is missing.");
            }
        }
    }

}
