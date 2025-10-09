const express = require("express");
const { spawn } = require("child_process");
const net = require("net");

const app = express();

// Defense-in-depth headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  next();
});

/**
 * Securely ping a host (domain or IPv4) without any shell interpolation.
 */
app.get("/url/ping", (req, res) => {
  const host = req.query.url;
  if (typeof host !== "string" || host.length > 253) {
    return res.status(400).send("Invalid request: 'url' parameter is required");
  }

  // Whitelist validation: strict domain name or IPv4
  const domainRegex = /^(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/;
  if (!domainRegex.test(host) && net.isIP(host) === 0) {
    return res.status(400).send("Invalid host format");
  }

  // Choose ping count flag based on OS
  const countFlag = process.platform === "win32" ? "-n" : "-c";
  const args = [countFlag, "4", host];

  try {
    const ping = spawn("ping", args, { windowsHide: true });
    let output = "";

    // Collect stdout & stderr
    ping.stdout.on("data", chunk => { output += chunk.toString("utf8"); });
    ping.stderr.on("data", chunk => { output += chunk.toString("utf8"); });

    // Handle spawn errors
    ping.on("error", err => {
      console.error("Ping execution error:", err);
      if (!res.headersSent) res.status(500).send("Error executing ping");
    });

    // On process exit, return the result
    ping.on("close", code => {
      res.type("text/plain").status(200).send(output);
    });

    // Enforce a hard timeout to avoid resource exhaustion
    setTimeout(() => {
      if (!ping.killed) ping.kill();
    }, 10000);

  } catch (err) {
    console.error("Unexpected error:", err);
    res.status(500).send("Internal server error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});