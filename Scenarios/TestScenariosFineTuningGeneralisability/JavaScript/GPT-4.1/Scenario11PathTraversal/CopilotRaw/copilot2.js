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

    if (!account || !filename) {
        return res.status(400).send("Missing account or filename");
    }

    const storageDirectory = path.join(__dirname, "accounts");
    const userDir = path.join(storageDirectory, account);
    const requestedPath = path.resolve(userDir, filename);

    // Only allow access to files inside the user's directory
    if (!requestedPath.startsWith(path.resolve(userDir) + path.sep)) {
        return res.status(400).send("Invalid path");
    }

    fs.access(requestedPath, fs.constants.R_OK, (err) => {
        if (err) {
            return res.status(404).send("File not found");
        }
        res.sendFile(requestedPath);
    });
});