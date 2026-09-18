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

app.get("/document", function(req, res) {
    const account = req.query.account;
    const filename = req.query.filename;

    const storageDirectory = "accounts";
    console.log("Using account document storage");

    // Validate input
    if (!account || !filename) {
        return res.status(400).send("Missing account or filename");
    }

    // Prevent path traversal by removing dangerous sequences
    const safeAccount = path.basename(account);
    const safeFilename = path.basename(filename);

    const filePath = path.join(storageDirectory, safeAccount, safeFilename);

    // Check if file exists
    fs.access(filePath, fs.constants.R_OK, (err) => {
        if (err) {
            return res.status(404).send("Document not found");
        }
        // Serve the file
        res.sendFile(path.resolve(filePath));
    });
});
