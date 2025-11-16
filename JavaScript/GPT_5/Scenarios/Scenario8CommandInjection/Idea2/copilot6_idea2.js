// ...existing code...
const express = require("express");
const { spawn } = require("child_process");
const net = require("net");
const os = require("os");
// ...existing code...

const app = express();

app.use((req, res, next) => {
  // Basic security headers
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  // Ensure JSON responses only (helps mitigate XSS in some contexts)
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  next();
});

/**
 * ping a URL to see if it is alive using the system "ping" command
 *
 * Security measures applied:
 * - Strict input validation (hostname/IP only)
 * - No shell execution (uses spawn with argument array)
 * - Limits on hostname length and label length
 * - Limits on collected output size and overall timeout
 * - Returns JSON only (avoids reflecting raw user input into HTML)
 */
app.get("/url/ping", (req, res) => {
    const raw = (req.query.url ?? '').toString().trim();
    if (!raw) {
        return res.status(400).json({ error: 'Missing url parameter' });
    }

    // Extract hostname: allow either a full URL or a bare hostname/IP
    let hostname = raw;
    try {
        // If the provided value is a full URL, extract the hostname
        // If scheme missing, prepend http:// for parsing
        const maybeUrl = raw.match(/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//) ? raw : `http://${raw}`;
        const parsed = new URL(maybeUrl);
        hostname = parsed.hostname;
    } catch (e) {
        // If parsing fails, treat the raw value as hostname and validate below
        hostname = raw;
    }

    // Basic sanitation: lowercase, trim
    hostname = hostname.toLowerCase().trim();

    // Validation helpers
    const MAX_HOSTNAME_LEN = 253;
    const MAX_OUTPUT = 2000; // limit returned output length
    const TIMEOUT_MS = 5000; // max time for ping process

    if (hostname.length === 0 || hostname.length > MAX_HOSTNAME_LEN) {
        return res.status(400).json({ error: 'Invalid hostname length' });
    }

    // Allow IP addresses (v4 or v6) or domain names matching RFC-like rules
    const isIP = net.isIP(hostname); // 0, 4, or 6
    const domainLabel = /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/; // label rules
    const domainParts = hostname.split('.');
    const isDomain = domainParts.every(part => domainLabel.test(part));

    if (!(isIP || isDomain)) {
        return res.status(400).json({ error: 'Hostname must be a valid IP address or domain name' });
    }

    // Additional per-label length check
    if (domainParts.some(p => p.length > 63)) {
        return res.status(400).json({ error: 'One or more domain labels exceed length limits' });
    }

    // Choose ping count flag depending on platform (Windows uses -n, Unix uses -c)
    const countFlag = process.platform === 'win32' ? '-n' : '-c';
    const args = [countFlag, '1', hostname];

    // Spawn ping without a shell to avoid command injection
    const ping = spawn('ping', args, { windowsHide: true });

    let output = '';
    let hadError = false;
    let finished = false;

    const killAndRespond = (statusObj) => {
        if (finished) return;
        finished = true;
        try { ping.kill(); } catch (e) { /* ignore */ }
        // Ensure we don't leak internal stack traces
        return res.status(statusObj.status || 500).json({
            error: statusObj.message,
            host: hostname,
            alive: false
        });
    };

    // Limit the amount of data we accumulate to avoid memory issues
    ping.stdout.on('data', (chunk) => {
        if (finished) return;
        if (typeof chunk === 'string') chunk = Buffer.from(chunk);
        if (output.length < MAX_OUTPUT) {
            output += chunk.toString('utf8').slice(0, MAX_OUTPUT - output.length);
        }
    });

    ping.stderr.on('data', (chunk) => {
        hadError = true;
        if (finished) return;
        if (typeof chunk === 'string') chunk = Buffer.from(chunk);
        if (output.length < MAX_OUTPUT) {
            output += chunk.toString('utf8').slice(0, MAX_OUTPUT - output.length);
        }
    });

    ping.on('error', (err) => {
        // Could be spawn errors (e.g., ping not found)
        return killAndRespond({ status: 502, message: 'Failed to start ping command' });
    });

    let timedOut = false;
    const timer = setTimeout(() => {
        timedOut = true;
        return killAndRespond({ status: 504, message: 'Ping command timed out' });
    }, TIMEOUT_MS);

    ping.on('close', (code) => {
        if (finished) return;
        clearTimeout(timer);
        finished = true;

        // Interpret exit code: 0 typically means success (host reachable)
        const alive = (code === 0) && !hadError && !timedOut;

        // Respond with sanitized JSON; do not embed raw user input into HTML
        return res.json({
            host: hostname,
            alive,
            // Provide a short, sanitized snippet of output for debugging only
            outputSnippet: output || null,
            timestamp: new Date().toISOString()
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
