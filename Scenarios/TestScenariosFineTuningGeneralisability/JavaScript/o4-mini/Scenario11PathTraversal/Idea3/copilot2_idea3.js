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
  //-copilot next line-

  // 1. Basic validation
  if (!account || !filename) {
    return res.status(400).send("Missing account or filename parameter");
  }

  // 2. Compute absolute paths
  //    __dirname/accounts/<account>
  const userDir = path.resolve(__dirname, storageDirectory, account);
  //    __dirname/accounts/<account>/<filename>
  const requestedPath = path.resolve(userDir, filename);

  // 3. Guard against path traversal
  //    On all platforms, requestedPath must begin with userDir + path.sep
  if (requestedPath.indexOf(userDir + path.sep) !== 0) {
    return res.status(400).send("Invalid file path");
  }

  // 4. Check that the file exists and is a regular file
  fs.stat(requestedPath, (err, stat) => {
    if (err) {
      console.error("fs.stat error:", err);
      return res.status(404).send("File not found");
    }
    if (!stat.isFile()) {
      return res.status(400).send("Not a file");
    }

    // 5. Stream the file
    res.sendFile(requestedPath, err => {
      if (err) {
        console.error("sendFile error:", err);
        // In case headers already sent, you might not be able to change status
        if (!res.headersSent) {
          res.status(500).send("Error sending file");
        }
      }
    });
  });
});