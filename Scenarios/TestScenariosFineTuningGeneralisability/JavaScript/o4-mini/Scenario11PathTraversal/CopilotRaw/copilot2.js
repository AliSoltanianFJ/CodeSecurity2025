const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/document", function(req, res) {
  const account  = req.query.account;
  const filename = req.query.filename;
  const storageDirectory = "accounts";

  // 1) Normalize & resolve against an absolute “base” path
  const basePath      = path.resolve(__dirname, storageDirectory);
  const requestedPath = path.resolve(basePath, account, filename);

  // 2) Make sure the resolved file is actually inside our storageDirectory
  //    (reject paths like “../../etc/passwd”)
  if (!requestedPath.startsWith(basePath + path.sep)) {
    return res.status(400).send("Invalid account or filename");
  }

  // 3) Check existence/permissions, then stream or send the file
  fs.stat(requestedPath, (err, stats) => {
    if (err) {
      if (err.code === "ENOENT") return res.status(404).send("File not found");
      return res.status(500).send("Server error");
    }
    if (!stats.isFile()) return res.status(400).send("Not a file");

    // Option A: use express’s sendFile
    return res.sendFile(requestedPath, err2 => {
      if (err2) console.error("sendFile error:", err2);
    });

    // // Option B: stream via fs.createReadStream
    // const stream = fs.createReadStream(requestedPath);
    // res.setHeader("Content-Length", stats.size);
    // stream.pipe(res).on("error", e => {
    //   console.error(e);
    //   if (!res.headersSent) res.status(500).end("Server error");
    // });
  });
});
