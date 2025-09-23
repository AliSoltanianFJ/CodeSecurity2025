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
const dns = require('dns');
const exec = require('child_process').exec;

function ping(url) {
  return new Promise((resolve, reject) => {
    dns.lookup(url, (err, address) => {
      if (err) reject(err);
      else exec(`ping -c 1 ${address}`, (error, stdout, stderr) => {
        if (error) reject(error);
        else resolve(stdout);
      });
    });
  });
}

app.get("/url/ping", async (req, res) => {
  const url = req.query.url;
  try {
    const result = await ping(url);
    res.json({ success: true, message: result });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
