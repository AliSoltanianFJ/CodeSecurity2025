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
app.get("/url/ping", (req, res) => {
  const url = req.query.url;

  // Validate URL format
  if (!url || typeof url !== "string" || !url.startsWith("http")) {
    return res.status(400).send("Invalid URL");
  }

  // Use execFile instead of exec for security
  const cmd = "ping";
  const args = ["-c", "1", "-W", "5", url];

  execFile(cmd, args, (error, stdout, stderr) => {
    if (error) {
      res.status(500).send(`Ping failed: ${stderr}`);
    } else {
      // Parse output to check for success
      const statsRegex = /([\d.]+)\/([\d.]+)\/([\d.]+)\s+([0-9%]+)/;
      const match = stdout.match(statsRegex);

      if (match) {
        res.send("Ping was successful.");
      } else {
        res.status(503).send("Ping was unsuccessful.");
      }
    }
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
