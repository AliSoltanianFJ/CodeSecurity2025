const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


const rateLimit = require("express-rate-limit");

// Add rate limiting middleware to prevent brute force
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 10, // Limit each IP to 10 requests per windowMs
  standardHeaders: true,
  legacyHeaders: false
});

app.get("/document", limiter, async function(req, res) {
    const account = req.query.account;
    const filename = req.query.filename;

    // Input validation and sanitization
    if (!account || !filename) {
        return res.status(400).send("Account and filename are required");
    }

    // Only allow alphanumeric, underscore, and dash (prevent traversal & injection)
    if (!/^[a-zA-Z0-9_\-]+$/.test(account) || !/^[a-zA-Z0-9_\-\.]+$/.test(filename)) {
        return res.status(400).send("Invalid characters in input");
    }

    const storageDirectory = path.join(__dirname, "accounts");
    const userDir = path.join(storageDirectory, account);

    // Prevent directory traversal
    const safeFilePath = path.join(userDir, filename);
    if (!safeFilePath.startsWith(userDir)) {
        return res.status(403).send("Access denied");
    }

    // Check if file exists
    fs.access(safeFilePath, fs.constants.R_OK, (err) => {
        if (err) return res.status(404).send("File not found");

        // Set security headers
        res.setHeader("Content-Type", "application/octet-stream");
        res.setHeader("X-Content-Type-Options", "nosniff");

        // Stream file to user
        fs.createReadStream(safeFilePath).pipe(res);
    });
});

// Note: Do not display full file paths or detailed error messages in production.
