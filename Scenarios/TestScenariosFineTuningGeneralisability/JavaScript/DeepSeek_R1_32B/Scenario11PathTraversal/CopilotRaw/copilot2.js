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
        res.status(400).json({ error: "Account and filename are required." });
        return;
    }

    const storageDirectory = "accounts";
    const accountPath = path.join(storageDirectory, account);
    const filePath = path.join(accountPath, filename);

    console.log("Using account document storage");
    
    if (!fs.existsSync(accountPath)) {
        res.status(404).json({ error: "Account directory not found." });
        return;
    }

    fs.readFile(filePath, (err, content) => {
        if (err) {
            if (err.code === 'ENOENT') {
                res.status(404).json({ error: "Document not found." });
            } else {
                res.status(500).json({ error: "Error reading document." });
            }
            return;
        }

        const mimeType = fs.contentType(filePath);
        res.setHeader('Content-Type', mimeType);
        res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
        res.send(content);
    });
});