const express = require("express");
const { spawn } = require("child_process");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  // additional safe headers
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  next();
});

// ...existing code...

/**
 * ping a URL to see if it is alive using the "ping" system command
 * Security measures:
 *  - validate and normalise input using the WHATWG URL parser
 *  - strict hostname validation (IP or RFC-like domain label checks)
 *  - avoid shell invocation by using spawn with shell: false
 *  - limit output size and impose a timeout
 *  - return JSON (prevents reflected XSS)
 */
app.get("/url/ping", (req, res) => {
  try {
    const raw = (req.query.url || '').toString().trim();
    if (!raw) {
      return res.status(400).json({ error: 'Missing url query parameter' });
    }

    // Limit length to avoid resource exhaustion
    if (raw.length > 255) {
      return res.status(400).json({ error: 'url parameter too long' });
    }

    // Normalise input by ensuring a scheme for the URL parser
    let normalized;
    try {
      normalized = new URL(raw.includes('://') ? raw : `http://${raw}`);
    } catch (e) {
      return res.status(400).json({ error: 'Invalid URL format' });
    }

    const host = normalized.hostname;
    if (!host || host.length === 0) {
      return res.status(400).json({ error: 'Could not determine hostname from URL' });
    }

    // Hostname validation:
    // Allow IP addresses (v4/v6) or domain names matching a conservative regex:
    // - labels 1..63 chars, letters/digits/hyphen, no leading/trailing hyphen
    // - total length <= 253
    if (net.isIP(host) === 0) {
      if (host.length > 253) {
        return res.status(400).json({ error: 'Hostname too long' });
      }
      const domainRegex = /^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$/;
      if (!domainRegex.test(host)) {
        return res.status(400).json({ error: 'Invalid hostname' });
      }
    }

    // Choose ping args per platform and avoid shell interpolation
    const isWin = process.platform === 'win32';
    const args = isWin ? ['-n', '1', host] : ['-c', '1', host];

    const MAX_OUTPUT = 4096; // limit captured output
    const TIMEOUT_MS = 5000; // kill ping if it takes too long

    const ping = spawn('ping', args, { shell: false, windowsHide: true });

    let stdout = '';
    let stderr = '';
    let timedOut = false;
    const killTimer = setTimeout(() => {
      timedOut = true;
      try { ping.kill(); } catch (e) {}
    }, TIMEOUT_MS);

    ping.stdout.on('data', (chunk) => {
      if (stdout.length < MAX_OUTPUT) {
        stdout += chunk.toString('utf8').slice(0, MAX_OUTPUT - stdout.length);
      }
    });

    ping.stderr.on('data', (chunk) => {
      if (stderr.length < MAX_OUTPUT) {
        stderr += chunk.toString('utf8').slice(0, MAX_OUTPUT - stderr.length);
      }
    });

    ping.on('error', (err) => {
      clearTimeout(killTimer);
      // Do not leak internal error details to client
      return res.status(500).json({ error: 'Failed to execute ping' });
    });

    ping.on('close', (code, signal) => {
      clearTimeout(killTimer);

      // Provide a safe, truncated response. No raw HTML, JSON only.
      const success = !timedOut && code === 0;
      const response = {
        host,
        success,
        timedOut: !!timedOut,
        exitCode: typeof code === 'number' ? code : null,
        signal: signal || null,
        output: stdout || stderr || null
      };

      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      return res.status(200).json(response);
    });
  } catch (err) {
    // Generic safe error response
    return res.status(500).json({ error: 'Internal server error' });
  }
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
