const express = require("express");
const { spawn } = require("child_process");
const dns = require("dns").promises;
const net = require("net");
const { URL, domainToASCII } = require("url");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

/**
 * Resolve a hostname (or return the IP if already an IP).
 */
async function resolveToIP(input) {
  const isIp = net.isIP(input);
  if (isIp) return { address: input, family: isIp };

  const { address, family } = await dns.lookup(input, { family: 0 });
  return { address, family };
}

/**
 * Extract a host token from a query value that might be:
 * - a raw hostname (e.g., example.com)
 * - an IP address (v4 or v6)
 * - a full URL (e.g., https://example.com/path)
 */
function extractHostToken(value) {
  if (typeof value !== "string") return null;
  const raw = value.trim();
  if (!raw || raw.length > 2048) return null;

  // If it's already an IP, return it directly.
  if (net.isIP(raw)) return raw;

  // Try to parse as URL; if no scheme, assume http for parsing only.
  let candidate = raw;
  try {
    const toParse = raw.includes("://") ? raw : `http://${raw}`;
    const u = new URL(toParse);
    if (!/^https?:$/i.test(u.protocol)) return null; // only http/https allowed
    candidate = u.hostname || "";
  } catch {
    // Not a URL; treat the input as a bare hostname
    candidate = raw;
  }

  candidate = candidate.trim();
  if (!candidate || candidate.includes(" ") || candidate.includes("/")) return null;

  // Convert IDN to ASCII safely
  const ascii = domainToASCII(candidate);
  if (!ascii) return null;

  // Light sanity checks for hostname tokens (not applied to IPs)
  if (!net.isIP(ascii)) {
    if (ascii.length > 253) return null;
    // Basic label checks without permitting spaces or control chars
    const valid = /^[A-Za-z0-9.-]+$/.test(ascii) && !ascii.startsWith("-") && !ascii.endsWith("-");
    if (!valid) return null;
    if (ascii.includes("..")) return null;
  }

  return ascii;
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", async (req, res) => {
  try {
    const q = req.query.url;
    const hostToken = extractHostToken(q);

    if (!hostToken) {
      return res.status(400).json({ error: "Invalid or missing url parameter." });
    }

    // Resolve to a concrete IP and use that as the only argument to ping.
    let resolved;
    try {
      resolved = await resolveToIP(hostToken);
    } catch {
      return res.json({ host: hostToken, alive: false });
    }

    const isIPv6 = resolved.family === 6;
    const cmd = "ping";
    const args = [];

    if (process.platform === "win32") {
      if (isIPv6) args.push("-6");
      // -n 1 => one echo request, -w 2000 => timeout in ms
      args.push("-n", "1", "-w", "2000", resolved.address);
    } else {
      if (isIPv6) args.push("-6");
      // -c 1 => one packet, -W 2 => 2s timeout (Linux/macOS use different flags; -W is seconds on Linux)
      args.push("-c", "1", "-W", "2", resolved.address);
    }

    const child = spawn(cmd, args, { windowsHide: true, stdio: "ignore" });

    let settled = false;
    const timer = setTimeout(() => {
      if (!settled) {
        settled = true;
        child.kill("SIGKILL");
        return res.json({ host: hostToken, alive: false, timeout: true });
      }
    }, 5000);

    child.on("error", () => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      return res.json({ host: hostToken, alive: false });
    });

    child.on("close", (code) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      const alive = code === 0;
      return res.json({ host: hostToken, alive });
    });
  } catch (e) {
    return res.status(500).json({ error: "Internal server error." });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
