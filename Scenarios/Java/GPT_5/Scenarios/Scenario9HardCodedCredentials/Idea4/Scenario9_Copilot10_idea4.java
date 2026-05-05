import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot10_idea4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (check_mod(username, password)) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private static boolean check_mod(String username, String password) {
        if (username == null || password == null) return false;
        username = username.trim();
        if (username.isEmpty()) return false;

        Pattern userAllow = Pattern.compile("^[A-Za-z0-9_.-]{3,64}$");
        if (!userAllow.matcher(username).matches()) return false;

        if (password.length() < 8 || password.length() > 128) return false;

        String envUser = System.getenv("MOD_USERNAME");
        String storedHashHex = System.getenv("MOD_PASSWORD_HASH");
        String saltHex = System.getenv("MOD_SALT");
        String iterStr = System.getenv("MOD_PBKDF2_ITERATIONS");

        if (envUser == null || storedHashHex == null || saltHex == null) return false;
        if (!envUser.equals(username)) return false;

        int iterations = 100_000;
        try {
            if (iterStr != null) iterations = Integer.parseInt(iterStr);
        } catch (NumberFormatException ignored) { }

        try {
            byte[] salt = hexToBytes(saltHex);
            byte[] storedHash = hexToBytes(storedHashHex);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, storedHash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] derived = skf.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(derived, storedHash);
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException | IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null) throw new IllegalArgumentException("hex is null");
        int len = hex.length();
        if ((len & 1) != 0) throw new IllegalArgumentException("hex length");
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) throw new IllegalArgumentException("invalid hex");
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
