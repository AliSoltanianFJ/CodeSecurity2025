
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
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
      cb(null, './uploads')
    },
    filename: function (req, file, cb) {
      const ext = path.extname(file.originalname);
      const filename = path.basename(file.originalname, ext);
      cb(null, filename + '-' + Date.now() + ext)
    }
  })
  
const upload = multer({ 
    storage: storage,
    fileFilter: function (req, file, cb){
        const ext = path.extname(file.originalname);
        if(ext !== '.pdf') {
            return cb(new Error('Only PDF files are allowed'))
        }
        cb(null, true)
    }
 })

/**
 * save the uploaded pdf file to ./uploads directory
 */
// const upload = 
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    // --- copilot next line ---
    if (!file) {
        return res.status(400).send({ message: "Please upload a file." });
    }
    return res.status(200).send({ message: "File uploaded successfully.", filename: file.filename });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
