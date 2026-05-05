const express = require("express");
const { URL } = require("url");
const dns = require("dns").promises;
const http = require("http");
const https = require("https");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

function isPrivateIPv4(addr) {
  const parts = addr.split(".").map((p) => Number(p));
  if (parts.length !== 4 || parts.some((n) => Number.isNaN(n))) return false;
  if (parts[0] === 10) return true;
  if (parts[0] === 127) return true;
  if (parts[0] === 169 && parts[1] === 254) return true;
  if (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) return true;
  if (parts[0] === 192 && parts[1] === 168) return true;
  return false;
}

function isPrivateIPv6(addr) {
  const a = addr.split("%")[0].toLowerCase();
  if (a === "::1") return true;
  if (a.startsWith("fe80")) return true; // link-local
  if (a.startsWith("fc") || a.startsWith("fd")) return true; // unique local
  return false;
}

app.get("/url/ping", async (req, res) => {
  const raw = req.query.url;
  if (typeof raw !== "string" || raw.length === 0 || raw.length > 2083) {
    return res.status(400).json({ error: "invalid request" });
  }

  let parsed;
  try {
    parsed = new URL(raw);
  } catch {
    return res.status(400).json({ error: "invalid url" });
  }

  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    return res.status(400).json({ error: "unsupported protocol" });
  }

  const hostname = parsed.hostname;
  try {
    const lookup = await dns.lookup(hostname, { all: false });
    const address = String(lookup.address);
    if (lookup.family === 4) {
      if (isPrivateIPv4(address)) return res.status(400).json({ error: "disallowed address" });
    } else if (lookup.family === 6) {
      if (isPrivateIPv6(address)) return res.status(400).json({ error: "disallowed address" });
    } else {
      return res.status(400).json({ error: "disallowed address" });
    }
  } catch {
    return res.status(400).json({ error: "unable to resolve host" });
  }

  const lib = parsed.protocol === "https:" ? https : http;
  const options = {
    method: "HEAD",
    hostname: parsed.hostname,
    port: parsed.port || (parsed.protocol === "https:" ? 443 : 80),
    path: parsed.pathname + (parsed.search || ""),
    timeout: 3000,
    headers: { "User-Agent": "health-check/1.0" },
  };

  const timer = setTimeout(() => {}, 0);
  try {
    const result = await new Promise((resolve, reject) => {
      const reqc = lib.request(options, (resp) => {
        resp.resume();
        resolve({ statusCode: resp.statusCode || 0 });
      });
      reqc.on("timeout", () => {
        reqc.destroy();
        reject(new Error("timeout"));
      });
      reqc.on("error", reject);
      reqc.end();
    });
    clearTimeout(timer);
    return res.status(200).json({ alive: true, status: result.statusCode });
  } catch {
    clearTimeout(timer);
    return res.status(502).json({ alive: false });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
