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

  // 1) Quick sanity check on inputs
  if (!account || !filename) {
    return res.status(400).send("Missing account or filename");
  }

  // Optional: enforce a tight naming policy 
  // (letters, numbers, dashes/underscores for accounts; add dot for file extns)
  const accountRe  = /^[A-Za-z0-9_-]+$/;
  const fileRe     = /^[A-Za-z0-9._-]+$/;
  if (!accountRe.test(account) || !fileRe.test(filename)) {
    return res.status(400).send("Invalid account or filename");
  }

  // 2) Build an absolute “root” for all account data:
  const rootDir = path.resolve(__dirname, storageDirectory);

  // 3) Resolve the user’s directory under that root:
  const userDir = path.resolve(rootDir, account);

  // 4) Reject if userDir is not actually under rootDir
  if (!userDir.startsWith(rootDir + path.sep)) {
    return res.status(400).send("Invalid account");
  }

  // 5) Resolve the full document path
  const documentPath = path.resolve(userDir, filename);

  // 6) Reject if that path is not under the userDir
  if (!documentPath.startsWith(userDir + path.sep)) {
    return res.status(400).send("Invalid filename");
  }

  // 7) Finally, check that the file exists and is a file
  fs.stat(documentPath, (err, stat) => {
    if (err) {
      // ENOENT, etc
      return res.status(404).send("File not found");
    }
    if (!stat.isFile()) {
      return res.status(400).send("Not a file");
    }

    // 8) Send it off!
    res.sendFile(documentPath, (err) => {
      if (err) {
        console.error("Error sending file:", err);
        // Note: headers may already be sent
        if (!res.headersSent) {
          res.status(500).send("Could not send file");
        }
      }
    });
  });
});
