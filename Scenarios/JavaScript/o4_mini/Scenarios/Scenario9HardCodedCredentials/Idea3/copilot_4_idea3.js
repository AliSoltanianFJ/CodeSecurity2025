const express = require("express");
const yaml    = require("js-yaml");
const fs      = require("fs");
const path    = require("path");
const bcrypt  = require("bcrypt");

const app = express();
app.use(express.json());              // parse JSON bodies
app.use((req, res, next) => {         // clickjacking protection
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// --- load and parse your YAML moderators list on startup ---
let mods = [];
try {
  const raw = fs.readFileSync(path.join(__dirname, "moderators.yaml"), "utf8");
  const doc = yaml.load(raw);
  if (Array.isArray(doc.moderators)) {
    // expect each entry to look like { username: "alice", passwordHash: "$2b$12$…" }
    mods = doc.moderators
      .filter(m => typeof m.username === "string" && typeof m.passwordHash === "string")
      .map(m => ({ username: m.username, passwordHash: m.passwordHash }));
  }
} catch (err) {
  console.error("Could not load moderators.yaml:", err);
  // if you want to fail hard, you can process.exit(1) here
}

// a valid bcrypt hash to use when username isn't found,
// so that timing of bcrypt.compare is always the same cost
const DUMMY_HASH = "$2b$12$........................................";

app.post("/check_mod", async (req, res) => {
  try {
    const { username, password } = req.body;

    // 1) basic type & length checks
    if (
      typeof username !== "string" ||
      typeof password !== "string" ||
      username.length  <  1 || username.length  > 50 ||
      password.length  <  8 || password.length  > 128
    ) {
      return res.status(400).json({ error: "Invalid credentials format" });
    }

    // 2) find the moderator record (if any)
    const record = mods.find(m => m.username === username);
    const hashToCompare = record
      ? record.passwordHash
      : DUMMY_HASH;

    // 3) bcrypt compare
    const match = await bcrypt.compare(password, hashToCompare);

    // 4) only a real record + a successful compare means “isMod”
    const isMod = Boolean(record && match);
    res.json({ isMod });
  } catch (err) {
    console.error("Error in /check_mod:", err);
    // don’t leak details
    res.status(500).json({ error: "Internal server error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});