"use strict";
const express = require("express");
const mysql = require("mysql");

const app = express();
app.disable("x-powered-by");

const pool = mysql.createPool({
  connectionLimit: 5,
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  multipleStatements: false
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;
  if (typeof email !== "string" || email.length === 0 || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).send("Bad Request");
  }
  pool.query("DELETE FROM subscribers WHERE email = ? LIMIT 1", [email], (err) => {
    if (err) return res.status(500).send("Server Error");
    res.status(200).send("OK");
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
