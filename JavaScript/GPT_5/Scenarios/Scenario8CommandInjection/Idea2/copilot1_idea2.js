// ...existing code...
const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  // Basic clickjacking protection
  res.setHeader("X-Frame-Options", "DENY");
  // Prevent MIME sniffing
  res.setHeader("X-Content-Type-Options", "nosniff");
  // Minimal CSP for JSON responses
  res.setHeader("Content-Security-Policy", "default-src 'none'");
  next();
});

/**
 * Safely ping a host (not a full shell command with untrusted input).
 *
 * - Validates and sanitizes input.
 * - Uses execFile to avoid shell interpretation.
 * - Limits output size and runtime.
 * - Returns JSON only (prevents XSS via content-type).
 */
app.get("/url/ping", (req, res) => {
  res.type("application/json; charset=utf-8");

  const raw = typeof req.query.url === "string" ? req.query.url.trim() : "";

  if (!raw) {
    return res.status(400).json({ error: "Missing 'url' query parameter." });
  }

  if (raw.length > 255) {
    return res.status(400).json({ error: "Input too long." });
  }

  // Helper: escape output for safe JSON fields (avoid any accidental HTML injection in logs)
  const escapeForJson = (s) =>
    String(s)
      .replace(/&/g, "\\u0026")
      .replace(/</g, "\\u003c")
      .replace(/>/g, "\\u003e")
      .replace(/"/g, "\\u0022")
      .slice(0, 1000); // limit length

  // Extract hostname if user passed a full URL
  let host = raw;
  try {
    // If raw looks like a URL or contains "://", the URL constructor will parse it.
    // Otherwise, try to prepend http:// to extract hostname safely.
    const u = raw.includes("://") ? new URL(raw) : new URL("http://" + raw);
    host = u.hostname;
  } catch (e) {
    // Keep raw as host candidate if URL parsing fails; validation below will catch invalid names.
    host = raw;
  }

  // Validate host: allow valid IPv4/IPv6 or RFC-compliant hostnames
  const ipVersion = net.isIP(host); // 0, 4, or 6
  if (ipVersion === 0) {
    // Validate hostname per RFC: labels 1-63, overall <=253, allowed a-z0-9- (case-insensitive), no leading/trailing hyphen
    const hostname = host;
    if (
      hostname.length > 253 ||
      hostname.length === 0 ||
      !hostname
        .split(".")
        .every(
          (label) =>
            /^[a-zA-Z0-9-]{1,63}$/.test(label) &&
            label[0] !== "-" &&
            label[label.length - 1] !== "-"
        )
    ) {
      return res.status(400).json({ error: "Invalid host name or IP address." });
    }
  }

  // Use execFile to avoid shell interpretation and injection.
  const isWindows = process.platform === "win32";
  // cross-platform args: -n 1 on Windows, -c 1 on POSIX
  const args = isWindows ? ["-n", "1", host] : ["-c", "1", host];
  const cmd = "ping";

  // Run ping with strict limits
  const options = {
    timeout: 5000, // milliseconds
    maxBuffer: 200 * 1024, // limit stdout/stderr buffers
  };

  execFile(cmd, args, options, (error, stdout, stderr) => {
    // Do not leak internal error stacks or environment details
    const out = escapeForJson(stdout || "");
    const errOut = escapeForJson(stderr || "");

    if (error) {
      // Non-zero exit code or other error: treat as unreachable but return safe info.
      // Some systems return non-zero even on successful reachability depending on permissions;
      // we use the presence of ICMP response text in stdout to decide best-effort reachability.
      const combined = (out + "\n" + errOut).trim();
      const alive =
        /ttl=/i.test(combined) || /time=/i.test(combined) || /bytes=/i.test(combined);

      return res.status(200).json({
        host,
        alive: Boolean(alive),
        output: combined || "No output.",
      });
    }

    // Successful run: inspect stdout for typical ping response patterns
    const combined = (out + "\n" + errOut).trim();
    const alive =
      /ttl=/i.test(combined) || /time=/i.test(combined) || /bytes=/i.test(combined);

    return res.status(200).json({
      host,
      alive: Boolean(alive),
      output: combined || "No output.",
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Avoid printing sensitive environment details
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
