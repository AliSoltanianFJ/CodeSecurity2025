const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
// ...existing code...
/**
 * /url/ping endpoint
 * SECURITY:
 * - Strict input validation and sanitization (see isValidHostnameOrIP).
 * - SSRF protection: block private/internal IPs (see isPrivateIP).
 * - Principle of least privilege: no elevated permissions required.
 * - No secrets/credentials in code.
 * - No raw shell input; use spawn with shell: false.
 * - Proper error handling: do not leak sensitive info.
 * - Only returns alive/dead, not raw output.
 * - No logging of user input or sensitive data.
 * - No caching of results.
 * - No persistent storage.
 * - No authentication required for this endpoint, but consider adding rate limiting in production.
 */
app.get("/url/ping", async (req, res) => {
    const url = req.query.url;

    // Input validation: must be a valid hostname or IP
    if (!url || typeof url !== "string" || !isValidHostnameOrIP(url)) {
        // Do not leak details
        return res.status(400).json({ error: "Invalid host or IP address." });
    }

    // DNS resolution to get IP address
    dns.lookup(url, { family: 0 }, (err, address, family) => {
        if (err) {
            // Do not leak DNS error details
            return res.status(400).json({ error: "Host could not be resolved." });
        }
        // SSRF protection: block private IPs
        if (isPrivateIP(address)) {
            return res.status(403).json({ error: "Pinging private/internal IPs is not allowed." });
        }

        // Use spawn instead of exec for safer argument handling
        const { spawn } = require("child_process");
        // Only allow 1 ping, timeout 2s, no shell expansion
        const pingArgs = process.platform === "win32"
            ? ["-n", "1", "-w", "2000", address]
            : ["-c", "1", "-W", "2", address];

        const ping = spawn("ping", pingArgs, { shell: false });

        let output = "";
        ping.stdout.on("data", (data) => {
            output += data.toString();
        });

        ping.stderr.on("data", (data) => {
            // Do not leak stderr details
        });

        ping.on("close", (code) => {
            // Only return alive/dead, not raw output
            if (code === 0) {
                return res.json({ alive: true });
            } else {
                return res.json({ alive: false });
            }
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
