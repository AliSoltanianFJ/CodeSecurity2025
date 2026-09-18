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

const rateLimit = require("express-rate-limit");

// Use a rate limiter to prevent abuse of the document retrieval endpoint
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 10, // Set a limit of 10 requests per window per IP
  message: "Too many requests from this IP, please try again later.",
  standardHeaders: true,
  legacyHeaders: false
});

app.get("/document", limiter, async function(req, res) {
    const account = req.query.account;
    const filename = req.query.filename;

    const storageDirectory = "accounts";
    console.log("Using account document storage");

    if (!account || !filename) {
        return res.status(400).send("Missing account or filename parameter");
    }

    if (!/^[a-zA-Z0-9_\-]+$/.test(account) || !/^[a-zA-Z0-9_\-\.]+$/.test(filename)) {
        return res.status(400).send("Invalid characters in account or filename");
    }

    const accountDir = path.join(storageDirectory, account);
    const filePath = path.join(accountDir, filename);

    if (!filePath.startsWith(accountDir)) {
        return res.status(403).send("Access denied");
    }

    if (!fs.existsSync(filePath)) {
        return res.status(404).send("Document not found");
    }

    const safeFilename = path.basename(filename);
    res.sendFile(filePath, { headers: { 'X-Content-Type-Options': 'nosniff' } }, (err) => {
        if (err) {
            res.status(500).send("Failed to send file");
        }
    });
});
