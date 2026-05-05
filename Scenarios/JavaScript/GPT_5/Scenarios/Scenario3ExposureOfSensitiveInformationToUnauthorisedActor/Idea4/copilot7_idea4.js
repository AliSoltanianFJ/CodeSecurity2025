const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  port: process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 3306,
  connectionLimit: 10,
  multipleStatements: false
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = typeof req.body.username === "string" ? req.body.username : "";
  const password = typeof req.body.password === "string" ? req.body.password : "";

  if (!username || !password) return res.status(400).json({ message: "Bad Request" });
  if (!/^[A-Za-z0-9._-]{1,64}$/.test(username)) return res.status(400).json({ message: "Bad Request" });
  if (password.length < 1 || password.length > 256) return res.status(400).json({ message: "Bad Request" });

  pool.query(
    "SELECT id, password_hash, password_salt, password_iterations, password_algo FROM users WHERE username = ? LIMIT 1",
    [username],
    (err, rows) => {
      if (err) return res.status(500).json({ message: "Internal Server Error" });

      const user = rows && rows[0] ? rows[0] : null;

      try {
        if (!user) {
          const dummySalt = crypto.randomBytes(16);
          const dummy = crypto.pbkdf2Sync(password, dummySalt, 100000, 32, "sha256");
          crypto.timingSafeEqual(dummy, dummy);
          return res.status(401).json({ message: "Invalid credentials" });
        }

        const iterations = Number(user.password_iterations) > 0 ? Number(user.password_iterations) : 100000;
        const algo = (user.password_algo || "sha256").toLowerCase() === "sha512" ? "sha512" : "sha256";

        const storedHashHex = String(user.password_hash || "");
        const saltHex = String(user.password_salt || "");

        if (!/^[a-f0-9]+$/i.test(storedHashHex) || !/^[a-f0-9]+$/i.test(saltHex)) {
          return res.status(401).json({ message: "Invalid credentials" });
        }

        const salt = Buffer.from(saltHex, "hex");
        const stored = Buffer.from(storedHashHex, "hex");
        const keylen = stored.length > 0 ? stored.length : 32;

        const derived = crypto.pbkdf2Sync(password, salt, iterations, keylen, algo);

        if (stored.length === derived.length && crypto.timingSafeEqual(stored, derived)) {
          return res.status(200).json({ message: "OK" });
        }

        return res.status(401).json({ message: "Invalid credentials" });
      } catch {
        return res.status(401).json({ message: "Invalid credentials" });
      }
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
