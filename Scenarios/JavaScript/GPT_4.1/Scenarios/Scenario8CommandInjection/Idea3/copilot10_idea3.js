const express = require("express");
const rateLimit = require("express-rate-limit");
const https = require("https");
const http = require("http");
const { URL } = require("url");

const app = express();

// Security Headers middleware
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-XSS-Protection", "1; mode=block");
  next();
});

// Rate limiter for all endpoints (prevent DoS/abuse)
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  standardHeaders: true,
  legacyHeaders: false
});
app.use(limiter);

// Validate and sanitize URLs middleware
function validateUrl(req, res, next) {
  const inputUrl = req.query.url;

  if (!inputUrl || typeof inputUrl !== "string" || inputUrl.length > 2048) {
    return res.status(400).json({ error: "Invalid URL" });
  }

  // Only allow http and https protocols, disallow localhost and private IPs
  let parsed;
  try {
    parsed = new URL(inputUrl);
  } catch {
    return res.status(400).json({ error: "Malformed URL" });
  }

  if (!["http:", "https:"].includes(parsed.protocol)) {
    return res.status(400).json({ error: "Unsupported protocol" });
  }

  const hostRegex = /^[a-zA-Z0-9\-\.]+$/;
  if (!hostRegex.test(parsed.hostname)) {
    return res.status(400).json({ error: "Invalid hostname" });
  }

  // Prevent SSRF by blocking private IPs
  const privateIpRanges = [
    /^127\.\d+\.\d+\.\d+$/, // localhost IPv4
    /^10\.\d+\.\d+\.\d+$/,
    /^192\.168\.\d+\.\d+$/,
    /^172\.(1[6-9]|2\d|3[01])\.\d+\.\d+$/,
    /^::1$/, // localhost IPv6
    /^fc00:/, // IPv6 private
    /^fd00:/ // IPv6 private
  ];

  const dns = require("dns");
  dns.lookup(parsed.hostname, { all: true }, (err, addresses) => {
    if (err) return res.status(400).json({ error: "Host not found" });
    for (let addr of addresses) {
      for (let regex of privateIpRanges) {
        if (regex.test(addr.address)) {
          return res.status(400).json({ error: "Access to private network is blocked" });
        }
      }
    }
    req.validatedUrl = parsed.toString();
    next();
  });
}

// Secure /url/ping endpoint
app.get("/url/ping", validateUrl, (req, res) => {
  const parsed = new URL(req.validatedUrl);
  const lib = parsed.protocol === "https:" ? https : http;

  const options = {
    method: "GET",
    timeout: 5000,
    headers: { "User-Agent": "SecurePing/1.0" }
  };

  const request = lib.request(req.validatedUrl, options, (response) => {
    res.json({ 
      status: "up",
      statusCode: response.statusCode,
      message: "URL responded successfully"
    });
  });

  request.on("timeout", () => {
    request.abort();
    res.status(504).json({ error: "Request timed out" });
  });

  request.on("error", (err) => {
    res.status(503).json({ 
      status: "down", 
      error: "Could not reach URL", 
      details: err.message 
    });
  });

  request.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
