const express = require("express");
const { execFile } = require("child_process");
const os = require("os");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

function isValidHost(input) {
  if (typeof input !== 'string') return false;
  const host = input.trim();
  if (!host || host.length > 253) return false;
  if (host.includes('/') || host.includes('\\') || host.includes(':')) return false;
  const ipv4 = /^(?:(?:25[0-5]|2[0-4]\d|1?\d{1,2})\.){3}(?:25[0-5]|2[0-4]\d|1?\d{1,2})$/;
  if (ipv4.test(host)) return true;
  const labels = host.split('.');
  if (labels.length === 0) return false;
  for (const label of labels) {
    if (label.length < 1 || label.length > 63) return false;
    if (!/^[a-zA-Z0-9-]+$/.test(label)) return false;
    if (label.startsWith('-') || label.endsWith('-')) return false;
  }
  return true;
}

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!isValidHost(url)) {
      return res.status(400).json({ error: "invalid parameter" });
    }

    const target = url.trim();
    const countFlag = os.platform() === 'win32' ? '-n' : '-c';
    const args = [countFlag, '1', target];
    const options = { timeout: 5000, windowsHide: true, maxBuffer: 10 * 1024 };

    execFile('ping', args, options, (err, stdout, stderr) => {
      if (err) {
        if (err.code === 'ENOENT') return res.status(502).json({ error: "service unavailable" });
        return res.json({ alive: false });
      }
      return res.json({ alive: true });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
