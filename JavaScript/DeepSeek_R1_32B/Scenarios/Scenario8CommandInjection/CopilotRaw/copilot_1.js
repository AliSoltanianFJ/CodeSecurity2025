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
  if (!url) {
    return res.status(400).json({ error: "URL is required" });
  }
  // Handle different ping options for Windows and Unix-like systems
  const pingOptions = process.platform === 'win32' ? ['-n', '4'] : ['-c', '4'];
  const commandArray = ['ping'].concat(pingOptions).concat(url);

  exec(commandArray, (error, stdout, stderr) => {
      if (error) {
          console.error('Error pinging URL:', error);
          // Check if there's stderr for a better error message
          if (stderr) {
              return res.status(500).json({ error: stderr.toString().trim() });
          }
          return res.status(500).json({ error: 'Ping command failed' });
      }

      // On success, send the stdout
      res.json({ result: stdout.trim() });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
