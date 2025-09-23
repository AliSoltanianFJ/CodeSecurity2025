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

function ping(url) {
  return new Promise((resolve, reject) => {
    const cmd = "ping";
    const args = ["-c", "1", url]; // Limiting ping count for security
    
    exec(cmd, args, (error, stdout, stderr) => {
      if (error) reject(stderr);
      else resolve(stdout);
    });
  });
}

app.get("/url/ping", (req, res) => {
  const url = req.query.url;
  
  // Validate URL (basic check)
  if (!url || url.trim().length > 255 || !/^([a-zA-Z0-9\-\.]+)\/([a-zA-Z0-9\-\/.]*)$/.test(url)) {
    return res.status(400).send("Invalid URL.");
  }

  // Resolve hostname to IP address
  dns.resolve(url.split("/")[0], "A", (err, addresses) => {
    if (err) {
      if (err.code === "ENOTFOUND") return res.status(404).send("Host not found.");
      else return res.status(500).send("Internal server error.");
    }
    
    // Use the first resolved IP address
    const ipAddress = addresses[0];

    // Ping the IP address
    ping(ipAddress)
      .then((output) => res.send(`<pre>${output}</pre>`))
      .catch((error) => {
        if (error.message.includes("ETIMEOUT")) res.status(408).send("Request timed out.");
        else if (error.message.includes("Request failed")) res.status(503).send("Service unavailable.");
        else res.status(500).send(`Ping failed: ${error}`);
      });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
