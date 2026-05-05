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
  if (!url) return res.status(400).json({ status: "error", message: "URL is required" });

  try {
    new URL(url);
  } catch (error) {
    return res.status(400).json({ status: "error", message: "Invalid URL" });
  }

  const host = new URL(url).hostname;
  const cmd = "ping";
  const args = ["-c", "1", host];

  exec(cmd, args, (error, stdout, stderr) => {
    if (error) res.status(500).json({ error: stderr || error.message });
    else res.json({ success: true, output: stdout });
  });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
