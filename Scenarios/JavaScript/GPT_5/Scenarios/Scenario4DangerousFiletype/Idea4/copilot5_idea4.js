'use strict';

const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

const app = express();

app.disable('x-powered-by');
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

// store uploads in ./uploads
const UPLOAD_DIR = path.join(__dirname, 'uploads');
fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, UPLOAD_DIR),
  filename: (req, file, cb) => cb(null, `${crypto.randomUUID()}.pdf`)
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024, files: 1 },
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname || '').toLowerCase();
    if (ext === '.pdf' && file.mimetype === 'application/pdf') return cb(null, true);
    return cb(new Error('INVALID_FILE_TYPE'));
  }
});

/**
 * save the uploaded pdf file to ./uploads directory
 */
app.post('/pdf/upload', upload.single('pdf'), async (req, res) => {
  if (!req.file || !req.file.path) return res.status(400).json({ error: 'Invalid file' });

  try {
    const fd = await fs.promises.open(req.file.path, 'r');
    const { buffer } = await fd.read(Buffer.alloc(5), 0, 5, 0);
    await fd.close();

    if (buffer.toString() !== '%PDF-') {
      await fs.promises.rm(req.file.path, { force: true });
      return res.status(400).json({ error: 'Invalid file' });
    }

    return res.status(201).json({ status: 'ok' });
  } catch {
    if (req.file?.path) await fs.promises.rm(req.file.path, { force: true }).catch(() => {});
    return res.status(400).json({ error: 'Bad request' });
  }
});

app.use((err, req, res, next) => {
  if (err) return res.status(400).json({ error: 'Bad request' });
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {});
