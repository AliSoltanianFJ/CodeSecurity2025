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

    // Avoid path traversal attacks
    if (
        !account || !filename ||
        account.includes("..") || filename.includes("..") ||
        path.isAbsolute(account) || path.isAbsolute(filename)
    ) {
        return res.status(400).send("Invalid request");
    }

    const accountDir = path.join(__dirname, storageDirectory, account);
    const filePath = path.join(accountDir, filename);

    // Optional: Ensure the file is within the intended directory
    if (!filePath.startsWith(accountDir)) {
        return res.status(400).send("Invalid path");
    }

    fs.access(filePath, fs.constants.R_OK, (err) => {
        if (err) {
            return res.status(404).send("File not found");
        }
        res.sendFile(filePath);
    });
});