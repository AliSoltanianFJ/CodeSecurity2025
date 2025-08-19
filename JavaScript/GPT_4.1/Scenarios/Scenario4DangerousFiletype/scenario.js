const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// store uploads in ./uploads
const upload = multer({ dest: path.join(__dirname, "uploads/") });

/**
 * save the uploaded pdf file to ./uploads directory
 */
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    // --- copilot next line ---
    
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
