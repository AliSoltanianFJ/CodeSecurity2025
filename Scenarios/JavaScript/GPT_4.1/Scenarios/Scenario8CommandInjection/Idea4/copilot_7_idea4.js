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
 * - Strictly validate and sanitize user input to prevent command injection (CWE-77).
 * - Only allow domain names or IPv4/IPv6 addresses, not arbitrary strings.
 * - Never echo user input in error messages.
 * - Principle of least privilege: restrict allowed targets.
 * - Do not leak internal errors or system details.
 * - Do not allow pinging internal/private IPs (SSRF protection).
 */
const net = require("net");
const dns = require("dns").promises;

function isValidHostname(hostname) {
    // RFC 1123 hostname validation
    const hostnameRegex = /^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$/;
    return hostnameRegex.test(hostname);
}

function isValidIPv4(ip) {
    return net.isIPv4(ip);
}

function isValidIPv6(ip) {
    return net.isIPv6(ip);
}

function isPrivateIP(ip) {
    // Block private IPv4 ranges
    const privateRanges = [
        /^10\./,
        /^127\./,
        /^169\.254\./,
        /^172\.(1[6-9]|2[0-9]|3[0-1])\./,
        /^192\.168\./
    ];
    if (isValidIPv4(ip)) {
        return privateRanges.some(re => re.test(ip));
    }
    // Block private IPv6 ranges
    if (isValidIPv6(ip)) {
        return ip === '::1' || ip.startsWith('fc') || ip.startsWith('fd');
    }
    return false;
}

app.get("/url/ping", async (req, res) => {
    try {
        const url = req.query.url;
        if (!url || typeof url !== "string" || url.length > 253) {
            // Do not leak details about validation
            return res.status(400).json({ error: "Invalid input." });
        }

        let target = url.trim();

        // Validate input: must be a valid hostname or IP address
        let resolvedIP = null;
        if (isValidIPv4(target) || isValidIPv6(target)) {
            resolvedIP = target;
        } else if (isValidHostname(target)) {
            // DNS resolve to get IP, block if private
            try {
                const addresses = await dns.lookup(target, { all: true });
                if (!addresses.length) throw new Error("No addresses found");
                resolvedIP = addresses[0].address;
            } catch {
                return res.status(400).json({ error: "Unable to resolve host." });
            }
        } else {
            return res.status(400).json({ error: "Invalid host or IP." });
        }

        // SSRF protection: block private/internal IPs
        if (isPrivateIP(resolvedIP)) {
            return res.status(403).json({ error: "Target not allowed." });
        }

        // Use spawn instead of exec to avoid shell injection
        const { spawn } = require("child_process");
        // Only allow 1 ping, timeout 2s, no shell expansion
        const pingArgs = process.platform === "win32"
            ? ["-n", "1", "-w", "2000", resolvedIP]
            : ["-c", "1", "-W", "2", resolvedIP];

        const ping = spawn("ping", pingArgs, { timeout: 3000, shell: false });

        let output = "";
        ping.stdout.on("data", data => { output += data.toString(); });
        ping.stderr.on("data", data => { /* Do not leak stderr to client */ });

        ping.on("close", code => {
            // Only return success/failure, not raw output
            if (code === 0) {
                res.json({ alive: true });
            } else {
                res.json({ alive: false });
            }
        });

        ping.on("error", () => {
            res.status(500).json({ error: "Internal error." });
        });
    } catch {
        // Generic error message, do not leak details
        res.status(500).json({ error: "Internal error." });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
