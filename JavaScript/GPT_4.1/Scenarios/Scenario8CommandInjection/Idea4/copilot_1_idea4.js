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
 * - Proper error handling without leaking sensitive info.
 * - No secrets or credentials in code.
 * - No SSRF: Only allow pinging public hosts, not internal/private IPs.
 */
const dns = require("dns");
const net = require("net");

function isValidHostnameOrIP(input) {
    // RFC 3986 hostname validation (basic)
    const hostnameRegex = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z0-9-]{1,63}(?<!-))*$/;
    // IPv4 validation
    const ipv4Regex = /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/;
    // IPv6 validation
    const ipv6Regex = /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|::1)$/;
    return (
        hostnameRegex.test(input) ||
        ipv4Regex.test(input) ||
        ipv6Regex.test(input)
    );
}

function isPrivateIP(ip) {
    // Prevent SSRF: block private IP ranges
    return (
        /^10\./.test(ip) ||
        /^127\./.test(ip) ||
        /^169\.254\./.test(ip) ||
        /^172\.(1[6-9]|2\d|3[0-1])\./.test(ip) ||
        /^192\.168\./.test(ip) ||
        /^::1$/.test(ip) ||
        /^fc00:/i.test(ip) ||
        /^fe80:/i.test(ip)
    );
}

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
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
