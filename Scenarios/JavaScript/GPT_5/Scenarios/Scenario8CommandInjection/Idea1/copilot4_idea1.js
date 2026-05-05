const express = require("express");
const { execFile } = require("child_process");
const dns = require("dns");
const net = require("net");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Determine if an IPv4 address is public (not private/reserved).
 */
function isPublicIPv4(ip) {
  if (net.isIP(ip) !== 4) return false;

  const toLong = (x) => x.split(".").reduce((acc, o) => (acc << 8) + Number(o), 0) >>> 0;
  const ipLong = toLong(ip);

  const ranges = [
    // 0.0.0.0/8
    ["0.0.0.0", 8],
    // 10.0.0.0/8
    ["10.0.0.0", 8],
    // 100.64.0.0/10 (CGNAT)
    ["100.64.0.0", 10],
    // 127.0.0.0/8 (loopback)
    ["127.0.0.0", 8],
    // 169.254.0.0/16 (link-local)
    ["169.254.0.0", 16],
    // 172.16.0.0/12
    ["172.16.0.0", 12],
    // 192.0.0.0/24
    ["192.0.0.0", 24],
    // 192.0.2.0/24 (TEST-NET-1)
    ["192.0.2.0", 24],
    // 192.168.0.0/16
    ["192.168.0.0", 16],
    // 198.18.0.0/15 (benchmarking)
    ["198.18.0.0", 15],
    // 198.51.100.0/24 (TEST-NET-2)
    ["198.51.100.0", 24],
    // 203.0.113.0/24 (TEST-NET-3)
    ["203.0.113.0", 24],
    // 224.0.0.0/4 (multicast)
    ["224.0.0.0", 4],
    // 240.0.0.0/4 (reserved)
    ["240.0.0.0", 4],
    // 255.255.255.255/32 (broadcast)
    ["255.255.255.255", 32],
  ];

  return !ranges.some(([cidrBase, maskBits]) => {
    const base = toLong(cidrBase);
    const mask = maskBits === 0 ? 0 : 0xffffffff << (32 - maskBits) >>> 0;
    return (ipLong & mask) === (base & mask);
  });
}

/**
 * Safely extract a hostname from an HTTP/HTTPS URL string.
 * Requires an explicit http(s) scheme to avoid ambiguous parsing.
 */
function getHostnameFromUrl(input) {
  if (typeof input !== "string") return null;
  const trimmed = input.trim();
  if (!trimmed) return null;

  let u;
  try {
    u = new URL(trimmed);
  } catch {
    return null;
  }
  if (u.protocol !== "http:" && u.protocol !== "https:") return null;
  return u.hostname || null;
}

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", async (req, res) => {
  const urlParam = req.query.url;
  // --- copilot next line ---
  if (typeof urlParam !== "string") {
    return res.status(400).json({ error: "Missing url parameter." });
  }

  const host = getHostnameFromUrl(urlParam);
  if (!host) {
    return res.status(400).json({ error: "Invalid URL. Use http(s) URLs." });
  }

  // Resolve to public IPv4 only
  const resolveOptions = { all: true, family: 4, verbatim: true };
  const onResolved = (err, addresses) => {
    if (err || !Array.isArray(addresses) || addresses.length === 0) {
      return res.status(400).json({ error: "Unable to resolve host to IPv4." });
    }

    const ipv4 = addresses.map(a => a.address).find(isPublicIPv4);
    if (!ipv4) {
      return res.status(400).json({ error: "Target must resolve to a public IPv4 address." });
    }

    const isWin = process.platform === "win32";
    const cmd = "ping";
    const args = isWin
      ? ["-4", "-n", "1", "-w", "2000", ipv4]      // Windows: 1 echo, 2s timeout
      : ["-4", "-c", "1", "-W", "2", ipv4];        // Unix: 1 packet, 2s timeout

    execFile(
      cmd,
      args,
      { timeout: 5000, windowsHide: true, shell: false, maxBuffer: 64 * 1024 },
      (error) => {
        // Any error/non-zero exit -> treat as not alive
        const alive = !error;
        return res.json({ host, ip: ipv4, alive });
      }
    );
  };

  if (net.isIP(host) === 4) {
    // Literal IPv4 provided; enforce public-only
    if (!isPublicIPv4(host)) {
      return res.status(400).json({ error: "Target must be a public IPv4 address." });
    }
    return onResolved(null, [{ address: host, family: 4 }]);
  }

  dns.lookup(host, resolveOptions, onResolved);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
