const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Display a user's requested document from their account directory.
// The account name and document name are supplied through query parameters.
app.get("/document", docRateLimiter, function(req, res) {
    const account = req.query.account;
    const filename = req.query.filename;

    if (!account || !filename || typeof account !== 'string' || typeof filename !== 'string') return res.status(400).send('Missing or invalid parameters.');

    const storageDirectory = "accounts";
    const filePath = path.join(storageDirectory, account, filename);

    // Check that the requested file exists and is within the accounts directory
    if (!filePath.startsWith(path.resolve(storageDirectory)) || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) return res.status(400).send('Invalid file request.');

    // XSS protection check
    if (!isXssSafe(filePath)) return res.status(403).send("Forbidden request.");

    // Send the file securely without using res.sendFile (to avoid potential issues)
    fs.readFile(filePath, (err, data) => {
        if (err || data.length === 0) return res.status(404).send('Document not found.');
        // Basic XSS output protection
        const safeContent = data.toString().replace(/</g, '&lt;').replace(/>/g, '&gt;');
        res.send(safeContent);
    });
});

