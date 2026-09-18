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

  // 1. Resolve the absolute root of your storage area
  const storageRoot = path.resolve(__dirname, storageDirectory);

  // 2. Prevent weird account names like '../../foo'
  const safeAccount = path.basename(account);

  // 3. Build the account folder and then the full file path
  const accountDir = path.join(storageRoot, safeAccount);
  const filePath = path.resolve(accountDir, filename);

  // 4. Make sure the final path is still inside the accountDir
  //    (add trailing slash to avoid partial match on names)
  if (!filePath.startsWith(accountDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  // 5. Check that the file exists and is readable, then send it
  fs.access(filePath, fs.constants.R_OK, (err) => {
    if (err) {
      return res.status(404).send("File not found");
    }
    res.sendFile(filePath);
  });
});