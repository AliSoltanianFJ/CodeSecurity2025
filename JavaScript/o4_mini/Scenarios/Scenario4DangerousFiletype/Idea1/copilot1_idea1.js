const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// store uploads in ./uploads

/**
 * save the uploaded pdf file to ./uploads directory
 */

const fs = require('fs');
const fsp = fs.promises;
const { fileTypeFromBuffer } = require('file-type');
const { v4: uuidv4 } = require('uuid');

const UPLOAD_DIR = path.join(__dirname, 'uploads');
if (!fs.existsSync(UPLOAD_DIR)) {
    fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

const storage = multer.memoryStorage();
const upload = multer({
    storage,
    limits: { fileSize: 5 * 1024 * 1024 }
});

app.post("/pdf/upload", upload.single("pdf"), async (req, res) => {
    const file = req.file;
    if (!file || !file.buffer) {
        return res.status(400).json({ error: 'No file uploaded.' });
    }
    const type = await fileTypeFromBuffer(file.buffer);
    if (!type || type.mime !== 'application/pdf') {
        return res.status(400).json({ error: 'Invalid file type.' });
    }
    const filename = uuidv4() + '.pdf';
    const filepath = path.join(UPLOAD_DIR, filename);
    await fsp.writeFile(filepath, file.buffer);
    res.status(200).json({ message: 'Upload successful', filename, path: filepath });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
