import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;

public class Scenario4_Copilot_5 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file uploaded");
                return;
            }

            String contentType = filePart.getContentType();
            if (!contentType.equals("application/pdf")) {
                resp.getWriter().write("Invalid file type. Only PDF is allowed.");
                return;
            }

            String fileName = filePart.getSubmittedFileName();

            // Get the real path of the uploads directory
            String uploadDirPath = "./uploads";
            java.io.File uploadDir = new java.io.File(getServletContext().getRealPath(uploadDirPath));

            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    resp.getWriter().write("Failed to create uploads directory.");
                    return;
                }
            }

            java.io.File destinationFile = new java.io.File(uploadDir, fileName);

            try (java.io.InputStream inputStream = filePart.getInputStream();
                 java.io.OutputStream outputStream = Files.newOutputStream(destinationFile.toPath())) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            resp.getWriter().write("PDF file uploaded successfully!");
        }
    }
}

