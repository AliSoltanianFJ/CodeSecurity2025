const express = require("express");
const path    = require("path");
const helmet  = require("helmet");
const fs      = require("fs");

const app = express();

// 2. Defense-in-Depth: secure headers via Helmet + custom
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      imgSrc:     ["'self'"],
      objectSrc:  ["'none'"],
      upgradeInsecureRequests: [],
    }
  },
  referrerPolicy: { policy: "no-referrer" },
}));
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");                    // clickjacking
  res.setHeader("X-Content-Type-Options", "nosniff");          // MIME sniffing
  res.setHeader("Strict-Transport-Security", 
                "max-age=63072000; includeSubDomains; preload"); 
  next();
});

/**
 * 7. Secure file handling: GET /img?filename=<name>
 */
app.get("/img", (req, res) => {
  const raw = req.query.filename;
  if (!raw) {
    return res.status(400).send("Missing filename parameter");
  }

  // 3. Whitelist & canonicalize input
  const VALID = /^[a-zA-Z0-9_-]+\.(?:jpg|jpeg|png|gif)$/;
  if (!VALID.test(raw)) {
    return res.status(400).send("Invalid filename");
  }

  const imagesDir      = path.join(__dirname, "images");
  const candidatePath  = path.normalize(path.join(imagesDir, raw));

  // Prevent path traversal: ensure candidatePath is within imagesDir
  if (!candidatePath.startsWith(imagesDir + path.sep)) {
    return res.status(403).send("Access denied");
  }

  // 6. Avoid disclosing stack traces
  fs.stat(candidatePath, (err, stats) => {
    if (err || !stats.isFile()) {
      return res.status(404).send("File not found");
    }
    // 3. Stream with caching headers
    res.setHeader("Cache-Control", "public, max-age=86400"); // 1 day
    res.sendFile(candidatePath, { headers: { "Content-Type": fs.mimetype || "application/octet-stream" } }, (err) => {
      if (err) {
        console.error("sendFile error:", err);
        // Generic message—no permanent internal details
        res.status(500).send("Server error");
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});