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
    
    // Construct the path to the document
    const filePath = path.join(storageDirectory, account, filename);
    
    // Check if the file exists
    if (!fs.existsSync(filePath)) {
        res.status(404).send("Document not found");
        return;
    }

    // Read the file
    const fileData = fs.readFileSync(filePath);
    
    // Set headers for the response
    const mimeType = Mime.getType(filePath); // You'll need to define or require a MIME type database
    res.setHeader('Content-Length', fileData.length);
    res.setHeader('Content-Type', mimeType || 'application/octet-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
    
    // Send the response with the file data
    res.send(fileData);
});