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
 * - Only allows IPv4/IPv6 addresses or valid hostnames (no URLs, no ports, no paths).
 * - Prevents command injection (CWE-77) by strict validation and never interpolating user input directly.
 * - Limits ping to 2 packets and 2 seconds timeout.
 * - Does not leak internal errors.
 * - Does not expose sensitive system info.
 * - Principle of least privilege: no elevated permissions.
 */
const dns = require("dns");
const net = require("net");

function isValidHostname(hostname) {
    // RFC 1123 hostname validation
    const hostnameRegex = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$/;
    return hostnameRegex.test(hostname);
}

function isValidIP(ip) {
    return net.isIP(ip) !== 0;
}

app.get("/url/ping", async (req, res) => {
    const target = req.query.url;
    // Validate input: only allow valid hostnames or IP addresses
    if (!target || !(isValidIP(target) || isValidHostname(target))) {
        // Do not reveal validation details to avoid info leaks
        return res.status(400).json({ error: "Invalid target." });
    }

    // Resolve hostname to IP to prevent SSRF to internal addresses
    try {
        let addresses = [];
        if (isValidIP(target)) {
            addresses = [target];
        } else {
            // Only resolve A/AAAA records, not CNAME or others
            addresses = await new Promise((resolve, reject) => {
                dns.resolve(target, (err, ips) => {
                    if (err) return reject(err);
                    resolve(ips);
                });
            });
        }
        // Filter out private/internal IPs (prevent SSRF)
        const privateRanges = [
            /^10\./, /^127\./, /^169\.254\./, /^172\.(1[6-9]|2[0-9]|3[0-1])\./, /^192\.168\./,
            /^::1$/, /^fc00:/, /^fe80:/, /^fd00:/ // IPv6 local
        ];
        const isPrivate = addresses.some(ip => privateRanges.some(re => re.test(ip)));
        if (isPrivate) {
            return res.status(403).json({ error: "Target not allowed." });
        }
        // Use first resolved IP
        const safeTarget = addresses[0];

        // Use spawn instead of exec for safer argument passing
        const { spawn } = require("child_process");
        // Windows: use 'ping -n 2 -w 2000'
        // Linux/macOS: use 'ping -c 2 -W 2'
        const isWin = process.platform === "win32";
        const pingCmd = isWin ? "ping" : "ping";
        const pingArgs = isWin
            ? [safeTarget, "-n", "2", "-w", "2000"]
            : ["-c", "2", "-W", "2", safeTarget];

        const ping = spawn(pingCmd, pingArgs, { timeout: 5000 });

        let output = "";
        let errorOutput = "";
        ping.stdout.on("data", data => { output += data.toString(); });
        ping.stderr.on("data", data => { errorOutput += data.toString(); });

        ping.on("close", code => {
            // Only return generic status, not raw output
            if (code === 0) {
                return res.json({ status: "alive" });
            } else {
                return res.json({ status: "unreachable" });
            }
        });

        ping.on("error", () => {
            // Do not leak error details
            return res.status(500).json({ error: "Ping failed." });
        });
    } catch (e) {
        // Do not leak DNS/system errors
        return res.status(500).json({ error: "Ping failed." });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
