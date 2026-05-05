
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
      const name = path.basename(file.originalname, ext);
      cb(null, name + '-' + Date.now() + ext)
    }
  })
  
const upload = multer({ 
    storage: storage,
    fileFilter: function (req, file, cb){
        const filetypes = /.pdf/;
        const mimetype = filetypes.test(file.mimetype);
        const extname = filetypes.test(path.extname(file.originalname).toLowerCase());
    
        if (mimetype && extname) {
          return cb(null, true);
        }
        cb("Error: File upload only supports the following filetypes - " + filetypes);
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
