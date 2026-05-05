const express = require('express');
const path = require('path');
const fs = require('fs');
const helmet = require('helmet');
const mime = require('mime-types');

const app = express();

// Layered security headers
app.use(helmet({
  frameguard: { action: 'deny' },
  hsts: { maxAge: 63072000, includeSubDomains: true, preload: true }
}));
app.use((req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  next();
});

/**
 * Serve images from the "images" directory.
 * GET /img?file=<filename>
 */
app.get('/img', (req, res) => {
  // 1. Validate presence and type
  const fileName = req.query.file;
  if (typeof fileName !== 'string' || fileName.trim() === '') {
    return res.status(400).json({ error: 'Missing file parameter' });
  }

  // 2. Strict whitelist: alphanumeric, underscore, hyphen, allowed extensions
  const validName = /^[A-Za-z0-9_-]+\.(?:jpg|jpeg|png|gif)$/i;
  if (!validName.test(fileName)) {
    return res.status(400).json({ error: 'Invalid file name' });
  }

  // 3. Normalize and enforce directory boundary
  const imagesDir = path.resolve(__dirname, 'images');
  const resolvedPath = path.resolve(imagesDir, fileName);
  if (!resolvedPath.startsWith(imagesDir + path.sep)) {
    return res.status(403).json({ error: 'Forbidden' });
  }

  // 4. Verify file exists and is a regular file
  fs.stat(resolvedPath, (err, stats) => {
    if (err || !stats.isFile()) {
      return res.status(404).json({ error: 'File not found' });
    }

    // 5. Determine MIME type and send securely
    const contentType = mime.lookup(resolvedPath) || 'application/octet-stream';
    res.setHeader('Content-Type', contentType);

    res.sendFile(resolvedPath, (sendErr) => {
      if (sendErr) {
        console.error('Error sending file:', sendErr);
        if (!res.headersSent) {
          res.status(500).json({ error: 'Internal Server Error' });
        }
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});