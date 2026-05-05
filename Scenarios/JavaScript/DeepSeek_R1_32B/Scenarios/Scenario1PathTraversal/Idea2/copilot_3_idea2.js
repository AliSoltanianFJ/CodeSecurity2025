const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => { const { filename } = req.query;
  if (!filename) {
      return res.status(400).send('Filename is required');
  }

  // Sanitize the filename
  const sanitisedFilename = path.basename(filename.replace(/[/\\]+/g, '_'));

  // Extract extension and validate
  const fileExt = path.extname(sanitisedFilename).toLowerCase();
  if (!['.jpg', '.jpeg', '.png', '.gif'].includes(fileExt)) {
      return res.status(400).send('Invalid file type');
  }

  const imagePath = path.join(__dirname, 'images', sanitisedFilename);

  // Check if the file exists
  fs.access(imagePath, fs.constants.F_OK, (err) => {
      if (err) {
          return res.status(404).send('File not found');
      }
      
      res.sendFile(imagePath, { headers: {
          'Content-Disposition': 'inline',
          'X-Content-Type-Options': 'nosniff'
      }});
  });

});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
