const express = require("express");
const dns = require("dns").promises;
const net = require("net");

const app = express();

// ...existing code...
app.use((req, res, next) => {
  // Basic clickjacking protection (already present).
  res.setHeader("X-Frame-Options", "DENY");

  // Restrictive content security policy to prevent XSS via returned HTML payloads.
  // We only serve JSON from this endpoint, but CSP is set as a defence-in-depth measure.
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");

  // Always serve JSON for the ping endpoint responses.
  res.setHeader("X-Content-Type-Options", "nosniff");

  next();
});
// ...existing code...

/**
 * Helper: check if an IPv4 address is private/reserved.
 */
function isPrivateIPv4(ip) {
  const parts = ip.split(".").map(Number);
  if (parts.length !== 4 || parts.some((n) => Number.isNaN(n))) return true; // treat malformed as private
  const [a, b] = parts;
  // 10.0.0.0/8
  if (a === 10) return true;
  // 172.16.0.0/12
  if (a === 172 && b >= 16 && b <= 31) return true;
  // 192.168.0.0/16
  if (a === 192 && b === 168) return true;
  // 127.0.0.0/8 loopback
  if (a === 127) return true;
  // 169.254.0.0/16 link-local
  if (a === 169 && b === 254) return true;
  return false;
}

/**
 * Helper: check if an IPv6 address is loopback or unique-local/link-local.
 * Simple textual checks are used (sufficient here to block private/local ranges).
 */
function isPrivateIPv6(ip) {
  if (!ip) return true;
  const lower = ip.toLowerCase();
  // loopback ::1
  if (lower === "::1" || lower === "0:0:0:0:0:0:0:1") return true;
  // Unique local addresses fc00::/7 (start with fc or fd)
  if (lower.startsWith("fc") || lower.startsWith("fd")) return true;
  // Link-local fe80::/10
  if (lower.startsWith("fe80")) return true;
  // IPv4-mapped IPv6 containing private IPv4 => treat as private
  const ipv4match = lower.match(/::ffff:(\d+\.\d+\.\d+\.\d+)$/);
  if (ipv4match && isPrivateIPv4(ipv4match[1])) return true;
  return false;
}

/**
 * Validate and sanitize the incoming "url" query parameter.
 * - limit length
 * - allow http/https only
 * - parse host name
 * - prevent localhost / private IPs by resolving DNS and checking addresses
 */
async function validateAndResolve(input) {
  if (!input || typeof input !== "string") {
    throw { status: 400, message: "Missing 'url' query parameter." };
  }

  const trimmed = input.trim();
  if (trimmed.length === 0 || trimmed.length > 200) {
    throw { status: 400, message: "Invalid 'url' length." };
  }

  // Try to parse as a URL. If no scheme provided, assume http for parsing convenience.
  let parsed;
  try {
    if (!/^[a-zA-Z][a-zA-Z0-9+\-.]*:\/\//.test(trimmed)) {
      parsed = new URL("http://" + trimmed);
    } else {
      parsed = new URL(trimmed);
    }
  } catch (err) {
    throw { status: 400, message: "Invalid URL format." };
  }

  const protocol = parsed.protocol.replace(":", "");
  if (!["http", "https"].includes(protocol)) {
    throw { status: 400, message: "Only http and https protocols are allowed." };
  }

  const hostname = parsed.hostname;
  if (!hostname || hostname.length > 253) {
    throw { status: 400, message: "Invalid hostname." };
  }

  // Resolve DNS to get addresses and ensure they are not private/local.
  let addresses;
  try {
    addresses = await dns.lookup(hostname, { all: true });
  } catch (err) {
    // DNS resolution failed
    throw { status: 404, message: "Hostname could not be resolved." };
  }

  if (!Array.isArray(addresses) || addresses.length === 0) {
    throw { status: 404, message: "No addresses found for hostname." };
  }

  // Filter out any private/reserved addresses to prevent SSRF to internal resources.
  const publicAddresses = addresses.filter((addr) => {
    if (addr.family === 4) {
      return !isPrivateIPv4(addr.address);
    } else if (addr.family === 6) {
      return !isPrivateIPv6(addr.address);
    }
    return false;
  });

  if (publicAddresses.length === 0) {
    throw { status: 403, message: "Resolved addresses are private or disallowed." };
  }

  // Return protocol, hostname and a single public address to use (first one).
  return {
    protocol,
    hostname,
    address: publicAddresses[0].address,
    family: publicAddresses[0].family,
  };
}

/**
 * ping-like endpoint that does NOT use shell commands.
 * It attempts a TCP connect to the resolved public IP on port 80 or 443.
 * Does not expose internal IPs in responses.
 */
app.get("/url/ping", async (req, res) => {
  try {
    const inputUrl = req.query.url;
    const info = await validateAndResolve(inputUrl);

    // Choose port depending on protocol. We do not rely on the client's input for port.
    const port = info.protocol === "https" ? 443 : 80;

    // Connection attempt with a timeout and safe resource handling.
    const timeoutMs = 3000;
    const start = Date.now();

    // Use a promise wrapper around net.connect
    const result = await new Promise((resolve) => {
      const socket = net.createConnection(
        { host: info.address, port, family: info.family, timeout: timeoutMs },
        () => {
          const latency = Date.now() - start;
          // Immediately destroy the socket once connected.
          socket.destroy();
          resolve({ alive: true, latencyMs: latency });
        }
      );

      socket.on("error", () => {
        // Do not return internal error details.
        socket.destroy();
        resolve({ alive: false });
      });

      socket.setTimeout(timeoutMs, () => {
        socket.destroy();
        resolve({ alive: false });
      });
    });

    // Return a minimal safe JSON response. Do not echo the resolved IP address.
    res.status(200).json({
      ok: true,
      url: info.hostname, // sanitized hostname only
      alive: !!result.alive,
      latencyMs: result.latencyMs ?? null,
    });
  } catch (err) {
    // Standardized safe error responses; avoid leaking stack traces or internal addresses.
    const status = err && err.status ? err.status : 500;
    const message =
      err && err.message
        ? err.message
        : "An unexpected error occurred while checking the URL.";
    res.status(status).json({ ok: false, error: message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
