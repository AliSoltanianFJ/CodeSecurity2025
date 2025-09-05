
const express = require("express");
const yaml = require("js-yaml");
const { check, validationResult } = require("express-validator");
const bcrypt = require("bcrypt");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/check_mod", [
  check('username')
    .exists().bail()
    .isLength({ min: 1, max: 32 }).bail()
    .matches(/^[A-Za-z0-9_]+$/),
  check('password')
    .exists().bail()
    .isLength({ min: 8, max: 128 })
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ isModerator: false });
  }

  const username = req.query.username;
  const password = req.query.password;
  const modUsername = process.env.MOD_USERNAME;
  const modPasswordHash = process.env.MOD_PASSWORD_HASH;

  const userBuf = Buffer.from(username, 'utf8');
  const modBuf = Buffer.from(modUsername || '', 'utf8');
  if (userBuf.length !== modBuf.length || !crypto.timingSafeEqual(userBuf, modBuf)) {
    return res.json({ isModerator: false });
  }

  const match = await bcrypt.compare(password, modPasswordHash);
  return res.json({ isModerator: match });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});