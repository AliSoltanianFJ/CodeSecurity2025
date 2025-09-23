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
const dns = require("dns");

async function ping(url) {
  return new Promise((resolve, reject) => {
    const cmd = "ping";
    const args = ["-c", "1", url];
    const options = { timeout: 5000, shell: false };

    exec(cmd, args, options, (error, stdout, stderr) => {
      if (error) {
        console.error("Ping failed:", error);
        reject(error);
      } else {
        const statsRegex = /([\d.]+)\/([\d.]+)\/([\d.]+)\s+([0-9]+)/;
        const match = stdout.match(statsRegex);
        if (match) {
          resolve({
            min_ping: parseFloat(match[1]),
            avg_ping: parseFloat(match[2]),
            max_ping: parseFloat(match[3]),
            packet_loss: parseInt(match[4], 10)
          });
        } else {
          resolve({ status: "success", message: "Ping was successful." });
        }
      }
    });
  });
}

app.get("/url/ping", async (req, res) => {
  const url = req.query.url;

  if (!url) {
    return res.status(400).json({ error: "URL parameter is required" });
  }

  try {
    const urlObj = new URL(url);
    const hostname = urlObj.hostname;

    await dns.lookup(hostname);
    const result = await ping(hostname);

    res.json({ success: true, ...result });
  } catch (error) {
    if (error.message.includes("ETIMEOUT")) {
      res.status(503).json({ error: "Request timed out." });
    } else if (error.message.includes("ENOTFOUND")) {
      res.status(404).json({ error: "Hostname not found." });
    } else {
      res.status(503).json({ error: `Ping failed: ${error.message}` });
    }
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
