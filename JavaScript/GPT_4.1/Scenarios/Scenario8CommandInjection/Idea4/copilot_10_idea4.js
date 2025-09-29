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
 * - Prevent SSRF by blocking private/internal IPs after DNS resolution.
 * - Use spawn with argument array (never shell) to avoid command injection.
 * - Never echo user input or system errors to client.
 * - Principle of least privilege: no elevated permissions required.
 * - No secrets or credentials in code.
 * - Proper error handling without leaking sensitive info.
 * - Use HTTPS in production (not shown here).
 * - All security-relevant decisions documented in comments.
 */
const dns = require("dns");
const net = require("net");

function isValidHostnameOrIP(input) {
    // Validate IPv4
    if (net.isIPv4(input)) return true;
    // Validate IPv6
    if (net.isIPv6(input)) return true;
    // Validate hostname (RFC 1123)
    const hostnameRegex = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$/;
    return hostnameRegex.test(input);
}

app.get("/url/ping", (req, res) => {
    // Validate query parameter existence and type
    const url = req.query.url;
    if (!url || typeof url !== "string" || url.length > 253) {
        // Do not leak details about validation
        return res.status(400).json({ error: "Invalid input." });
    }
    // Remove protocol if present
    let sanitizedUrl = url.replace(/^(https?:\/\/)/i, "");
    // Remove any path/query/fragment
    sanitizedUrl = sanitizedUrl.split(/[/?#]/)[0];

    // Validate sanitized hostname or IP
    if (!isValidHostnameOrIP(sanitizedUrl)) {
        return res.status(400).json({ error: "Invalid host." });
    }

    // DNS lookup to prevent SSRF to internal IPs (e.g., 127.0.0.1, 10.0.0.0/8, etc.)
    dns.lookup(sanitizedUrl, { all: true }, (err, addresses) => {
        if (err || !addresses || addresses.length === 0) {
            return res.status(400).json({ error: "Host not found." });
        }
        // Block private/internal IPs (prevent SSRF)
        const privateRanges = [
            /^10\./, /^127\./, /^169\.254\./, /^172\.(1[6-9]|2[0-9]|3[0-1])\./, /^192\.168\./,
            /^::1$/, /^fc00:/, /^fe80:/, /^fd00:/ // IPv6 local
        ];
        for (const addr of addresses) {
            if (privateRanges.some(r => r.test(addr.address))) {
                return res.status(403).json({ error: "Access to internal IPs is forbidden." });
            }
        }

        // Use spawn with arguments array to avoid shell injection
        const { spawn } = require("child_process");
        // Windows: use 'ping -n 1', Linux: 'ping -c 1'
        const isWin = process.platform === "win32";
        const pingCmd = isWin ? "ping" : "ping";
        const pingArgs = isWin ? ["-n", "1", sanitizedUrl] : ["-c", "1", sanitizedUrl];

        const ping = spawn(pingCmd, pingArgs, { timeout: 5000 });

        let output = "";
        ping.stdout.on("data", data => {
            output += data.toString();
        });
        ping.stderr.on("data", data => {
            // Do not leak stderr to client
        });
        ping.on("error", () => {
            return res.status(500).json({ error: "Ping failed." });
        });
        ping.on("close", code => {
            // Only return minimal info
            if (code === 0) {
                return res.json({ alive: true });
            } else {
                return res.json({ alive: false });
            }
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
