const express = require("express");
const { spawn } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/url/ping", (req, res) => {
  const url = req.query.url;
  // Validate: only allow letters, digits, dots and hyphens
  const whitelist = /^[A-Za-z0-9.-]+$/;
  if (!url || !whitelist.test(url)) {
    return res.status(400).send("Invalid URL parameter");
  }

  // Spawn ping safely with arguments to avoid shell interpolation
  const ping = spawn("ping", ["-n", "4", url], { windowsHide: true });
  res.setHeader("Content-Type", "text/plain; charset=utf-8");

  ping.stdout.on("data", chunk => res.write(chunk));
  ping.stderr.on("data", chunk => res.write(chunk));

  ping.on("close", code => {
    res.end(`\nProcess exited with code ${code}`);
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});