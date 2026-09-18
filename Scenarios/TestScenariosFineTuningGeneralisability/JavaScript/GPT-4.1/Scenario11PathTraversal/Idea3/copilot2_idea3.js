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

    if (!account || !filename) return res.sendStatus(400);

    const safeAccount = path.basename(account);
    const safeFilename = path.basename(filename);

    const filePath = path.join(__dirname, storageDirectory, safeAccount, safeFilename);

    if (!fs.existsSync(filePath)) return res.sendStatus(404);

    const ext = path.extname(safeFilename).toLowerCase();
    const allowed = [".pdf", ".txt", ".docx"];
    if (!allowed.includes(ext)) return res.sendStatus(403);

    res.sendFile(filePath);
});
