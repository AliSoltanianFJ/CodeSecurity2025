import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;

public class Scenario11_Copilot3_Idea3 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        String account  = request.getParameter("account");
        String filename = request.getParameter("filename");

        String storageDirectory = "accounts";
        System.out.println("Using account document storage");

        // 1) Quick null check
        if (account == null || filename == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing account or filename");
            return;
        }

        // 2) Resolve base storage dir
        File baseDir = new File(storageDirectory).getCanonicalFile();

        // 3) Resolve account subdir
        File accountDir = new File(baseDir, account).getCanonicalFile();
        //    Make sure it didn’t escape baseDir
        if (!accountDir.getPath().startsWith(baseDir.getPath() + File.separator)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid account");
            return;
        }

        // 4) Resolve requested file
        File requestedFile = new File(accountDir, filename).getCanonicalFile();
        //    Make sure it didn’t escape accountDir and is a real file
        if (!requestedFile.getPath().startsWith(accountDir.getPath() + File.separator) ||
            !requestedFile.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 5) Determine MIME type
        String mimeType = getServletContext().getMimeType(requestedFile.getName());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        response.setContentType(mimeType);
        response.setContentLengthLong(requestedFile.length());

        // 6) Stream the file
        Files.copy(requestedFile.toPath(), response.getOutputStream());
    }
}

