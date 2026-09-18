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
  const account  = req.query.account;
  const filename = req.query.filename;
  const storageDirectory = "accounts";

  if (!account || !filename) {
    return res.status(400).send("Missing account or filename");
  }

  // resolve the base storage dir and the user's directory
  const baseDir = path.resolve(storageDirectory);
  const userDir = path.resolve(baseDir, account);

  // make sure userDir is actually inside baseDir
  if (!userDir.startsWith(baseDir + path.sep)) {
    return res.status(403).send("Access denied");
  }

  // resolve the requested file path
  const filePath = path.resolve(userDir, filename);

  // make sure the file is actually inside the user's dir
  if (!filePath.startsWith(userDir + path.sep)) {
    return res.status(403).send("Access denied");
  }

  // now check the file exists and is a regular file
  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      return res.status(404).send("Document not found");
    }
    // safely send it
    res.sendFile(filePath);
  });
});