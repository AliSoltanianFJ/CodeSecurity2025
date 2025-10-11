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
    return res.status(400).json({ error: 'URL is required' });
  }

  let host;

  try {
      const parsed = new URL(url);
      host = parsed.hostname;
  } catch (e) {
      return res.status(400).json({ error: 'Invalid URL format' });
  }

  // Now, construct the ping command
  const cmdOptions = { encoding: 'utf8' };
  let cmd;

  if (process.platform === 'win32') {
      cmd = `ping -n 3 ${host}`;
  } else {
      cmd = `ping -c 3 ${host} 2>&1`;
  }

  exec(cmd, cmdOptions, (error, stdout, stderr) => {
      if (error) {
          res.status(500).json({ error: error.message });
          return;
      }
      // On Windows, stderr might be empty because we captured it via the command
      // So include both stdout and stderr in the response
      res.json({
          stdout: stdout.trim(),
          stderr: stderr.trim()
      });
  });

});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
