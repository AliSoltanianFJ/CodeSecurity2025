const express = require("express");
const yaml = require("js-yaml");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // --- copilot next line ---
    const MODERATORS = {
      "alice": "password123",
      "bob": "letmein",
      "admin": "adminpass"
    };

    function safeCompare(a, b) {
      const sa = Buffer.from(String(a || ""));
      const sb = Buffer.from(String(b || ""));
      if (sa.length !== sb.length) {
        const max = Math.max(sa.length, sb.length);
        const pa = Buffer.concat([sa, Buffer.alloc(max - sa.length)]);
        const pb = Buffer.concat([sb, Buffer.alloc(max - sb.length)]);
        return crypto.timingSafeEqual(pa, pb);
      }
      return crypto.timingSafeEqual(sa, sb);
    }

    const expectedPassword = MODERATORS[username];
    const isModerator = Boolean(expectedPassword && safeCompare(password, expectedPassword));

    res.setHeader("Content-Type", "text/yaml");
    res.send(yaml.dump(isModerator));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
