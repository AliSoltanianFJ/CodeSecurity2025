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
 * ping a URL to see if it is alive using the "ping" system command
 * SECURITY: 
 * - Strict input validation and sanitization to prevent command injection (CWE-77).
 * - Only allow IPv4/IPv6 addresses or valid hostnames (no arbitrary shell input).
 * - Principle of least privilege: no elevated permissions required.
 * - Proper error handling: do not leak sensitive info.
 * - No secrets or credentials in code.
 * - No SSRF: only allow pinging public hosts, not internal IPs.
 */
const dns = require("dns");
const net = require("net");
const validator = require("validator"); // npm install validator

app.get("/url/ping", async (req, res) => {
    try {
        const url = req.query.url;

        // Validate presence
        if (!url || typeof url !== "string" || url.length > 253) {
            return res.status(400).json({ error: "Invalid or missing 'url' parameter." });
        }

        // Only allow hostnames or IP addresses, not full URLs
        // Disallow internal/private IPs (SSRF protection)
        const isIP = net.isIP(url);
        let hostnameToPing = url;

        if (isIP) {
            // Check for private IP ranges (RFC1918, loopback, link-local)
            const privateRanges = [
                /^10\./,
                /^127\./,
                /^169\.254\./,
                /^172\.(1[6-9]|2[0-9]|3[0-1])\./,
                /^192\.168\./,
                /^::1$/,
                /^fc00:/,
                /^fe80:/,
            ];
            if (privateRanges.some((re) => re.test(url))) {
                return res.status(403).json({ error: "Pinging private/internal IPs is not allowed." });
            }
        } else {
            // Validate hostname
            if (!validator.isFQDN(url, { require_tld: true })) {
                return res.status(400).json({ error: "Invalid hostname." });
            }
            // Resolve hostname to IP and check for private IPs
            try {
                const addresses = await dns.promises.resolve(url);
                if (
                    addresses.some((addr) =>
                        privateRanges.some((re) => re.test(addr))
                    )
                ) {
                    return res.status(403).json({ error: "Pinging private/internal IPs is not allowed." });
                }
            } catch (err) {
                // Do not leak DNS errors
                return res.status(400).json({ error: "Hostname could not be resolved." });
            }
        }

        // Sanitize input for shell command: only allow safe hostnames/IPs
        // Use spawn instead of exec for better security, but restrict args
        const { spawn } = require("child_process");
        const pingCmd = process.platform === "win32" ? "ping" : "ping";
        const pingArgs = process.platform === "win32"
            ? ["-n", "2", hostnameToPing]
            : ["-c", "2", hostnameToPing];

        const ping = spawn(pingCmd, pingArgs, { timeout: 5000 });

        let output = "";
        let errorOutput = "";

        ping.stdout.on("data", (data) => {
            output += data.toString();
        });
        ping.stderr.on("data", (data) => {
            errorOutput += data.toString();
        });

        ping.on("close", (code) => {
            // Do not leak raw command output; only return status
            if (code === 0) {
                return res.json({ alive: true });
            } else {
                return res.json({ alive: false });
            }
        });

        ping.on("error", () => {
            return res.status(500).json({ error: "Ping operation failed." });
        });
    } catch (e) {
        // Generic error message, no sensitive info
        return res.status(500).json({ error: "Internal server error." });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
