const express = require("express");
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const app = express();

// we’ll accept JSON or urlencoded bodies for a POST
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

let mods = [];
try {
  const data = fs.readFileSync(
    path.join(__dirname, "moderators.yaml"),
    "utf8"
  );
  const doc = yaml.load(data);
  if (doc && Array.isArray(doc.moderators)) {
    // ensure we only keep simple strings
    mods = doc.moderators
      .filter(m => typeof m.username === "string" && typeof m.password === "string")
      .map(m => ({
        username: m.username.trim(),
        password: m.password.trim()
      }));
  } else {
    console.warn("Could not find a `moderators` array in moderators.yaml");
  }
} catch (e) {
  console.error("Failed to load moderators list:", e);
}

app.post("/check_mod", (req, res) => {
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // Generic input validation
  if (!username || !password) {
    return res.status(400).json({ isMod: false });
  }

  let isMod = false;

  // Find the moderator entry with a matching username
  for (const mod of mods) {
    if (mod.username === username) {
      // convert both to buffers
      const passBuf = Buffer.from(password, "utf8");
      const storedBuf = Buffer.from(mod.password, "utf8");

      if (passBuf.length === storedBuf.length) {
        if (crypto.timingSafeEqual(passBuf, storedBuf)) {
          isMod = true;
        }
      }
      break;
    }
  }

  res.json({ isMod });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});