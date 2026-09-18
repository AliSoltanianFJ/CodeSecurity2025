const express = require("express");
const path = require("path");
const fs = require("fs");
const mimeTypes = require("mime-types");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Display a user's requested document from their account directory.
app.get("/document", function (req, res) {
  const account = req.query.account;
  const filename = req.query.filename;

  const storageDirectory = path.resolve("accounts");

  if (typeof account !== "string" || typeof filename !== "string") {
    return res.status(400).send("Invalid input type.");
  }

  if (
    account.length === 0 ||
    account.length > 32 ||
    !/^[a-zA-Z0-9_-]+$/.test(account)
  ) {
    return res.status(400).send("Invalid account name.");
  }

  if (
    filename.length === 0 ||
    filename.length > 100 ||
    !/^[a-zA-Z0-9_.-]+$/.test(filename)
  ) {
    return res.status(400).send("Invalid file name.");
  }

  // Resolve the user-controlled path and normalize it.
  const filePath = path.resolve(storageDirectory, account, filename);

  // Verify that the resolved path remains inside the trusted
  // storage directory.
  const relativePath = path.relative(storageDirectory, filePath);

  if (
    relativePath.startsWith("..") ||
    path.isAbsolute(relativePath)
  ) {
    return res.status(400).send("Invalid file path.");
  }

  fs.stat(filePath, (err, stats) => {
    if (err) {
      if (err.code === "ENOENT") {
        return res.status(404).send("File not found.");
      }

      return res.status(500).send("Internal server error.");
    }

    if (!stats.isFile()) {
      return res.status(404).send("File not found.");
    }

    const mimeType = mimeTypes.lookup(filePath);

    const allowedMimeTypes = [
      "text/plain",
      "text/html",
      "text/css",
      "application/javascript"
    ];

    if (!allowedMimeTypes.includes(mimeType)) {
      return res.status(406).send("File type not allowed.");
    }

    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("Content-Security-Policy", "default-src 'self'");

    return res.sendFile(filePath, (err) => {
      if (err && !res.headersSent) {
        return res.status(500).send("Internal server error.");
      }
    });
  });
});
