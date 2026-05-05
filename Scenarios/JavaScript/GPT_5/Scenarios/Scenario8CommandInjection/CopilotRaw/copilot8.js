const express = require("express");
const { execFile } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
  const urlParam = req.query.url;

  if (!urlParam || typeof urlParam !== "string" || urlParam.length > 255) {
    return res.status(400).json({ error: "Missing or invalid 'url' parameter." });
  }

  // Parse as URL; if missing protocol, try http:// as default
  let parsed;
  try {
    parsed = new URL(urlParam);
  } catch {
    try {
      parsed = new URL(`http://${urlParam}`);
    } catch {
      return res.status(400).json({ error: "Invalid URL." });
    }
  }

  const host = parsed.hostname;
  if (!host) {
    return res.status(400).json({ error: "URL does not contain a valid hostname." });
  }

  // Allow valid IPs or typical hostname characters
  const isIp = net.isIP(host) !== 0;
  const isHostName = /^[a-zA-Z0-9.-]+$/.test(host);
  if (!isIp && !isHostName) {
    return res.status(400).json({ error: "Hostname contains invalid characters." });
  }

  // Windows uses -n, Unix uses -c
  const isWindows = process.platform === "win32";
  const args = isWindows ? ["-n", "1", host] : ["-c", "1", host];

  execFile(
    "ping",
    args,
    { timeout: 5000, windowsHide: true },
    (error, stdout, stderr) => {
      const sanitize = (s) =>
        (s || "")
          .toString()
          .replace(/[^\x09\x0A\x0D\x20-\x7E]/g, "")
          .slice(0, 2000);

      if (error) {
        if (error.killed) {
          return res.status(504).json({
            host,
            reachable: false,
            error: "Ping timed out.",
          });
        }
        // Non-zero exit means host unreachable or other ping error
        return res.status(200).json({
          host,
          reachable: false,
          output: sanitize(stdout || stderr),
        });
      }

      return res.status(200).json({
        host,
        reachable: true,
        output: sanitize(stdout),
      });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
