from flask import Flask, request, jsonify
from werkzeug.security import generate_password_hash
import re
import os
import mysql.connector
from mysql.connector import errorcode, IntegrityError

app = Flask(__name__)

# Configuration pulled from environment variables to avoid hardcoding secrets
DB_CONFIG = {
    "host": os.environ.get("DB_HOST", "localhost"),
    "user": os.environ.get("DB_USER", "appuser"),
    "password": os.environ.get("DB_PASSWORD", ""),
    "database": os.environ.get("DB_NAME", "appdb"),
    "port": int(os.environ.get("DB_PORT", 3306)),
    "charset": "utf8mb4",
    "use_unicode": True,
    # avoid insecure auth plugins if possible; let connector choose secure defaults
}

# Validation rules
USERNAME_REGEX = re.compile(r"^[A-Za-z0-9_]{3,30}$")  # only allow letters, digits, underscore
MIN_PASSWORD_LENGTH = 12
MAX_PASSWORD_LENGTH = 128

@app.route('/register', methods=['POST'])
def register():
    # Ensure request contains form data
    if not request.form:
        return jsonify({"error": "Invalid request"}), 400

    # Retrieve and trim inputs
    username = (request.form.get('username') or "").strip()
    password = request.form.get('password') or ""

    # Basic presence checks
    if not username or not password:
        return jsonify({"error": "Username and password are required"}), 400

    # Enforce length limits to avoid excessive resource usage (defends against some DoS/buffer issues)
    if len(username) > 30 or len(password) > MAX_PASSWORD_LENGTH:
        return jsonify({"error": "Input too long"}), 400

    # Validate username structure (prevents injection and unexpected characters)
    if not USERNAME_REGEX.fullmatch(username):
        return jsonify({"error": "Invalid username. Use 3-30 letters, digits or underscore only."}), 400

    # Validate password complexity
    if len(password) < MIN_PASSWORD_LENGTH or not re.search(r"[A-Za-z]", password) or not re.search(r"\d", password):
        return jsonify({"error": "Password must be at least 12 characters and include letters and numbers"}), 400

    # Hash the password using a secure, up-to-date algorithm (PBKDF2 with SHA-256)
    # werkzeug.generate_password_hash provides a salted, iterated hash
    try:
        password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    except Exception:
        # Do not reveal implementation details
        return jsonify({"error": "Server error"}), 500

    # Do not keep plaintext password in memory longer than necessary
    # (rebind to a short-lived variable and allow GC)
    password = None

    # Insert the new user using parameterized queries to prevent SQL injection
    conn = None
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        insert_sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(insert_sql, (username, password_hash))
        conn.commit()
        cursor.close()
        # Successful creation; do not echo back private info
        return jsonify({"status": "created"}), 201

    except IntegrityError as ie:
        # Common case: duplicate username (unique constraint)
        # Do not reveal DB specifics
        if ie.errno == errorcode.ER_DUP_ENTRY:
            return jsonify({"error": "Username already exists"}), 409
        return jsonify({"error": "Registration failed"}), 400

    except mysql.connector.Error:
        # Generic DB error; do not leak internals
        return jsonify({"error": "Server error"}), 500

    finally:
        if conn is not None and conn.is_connected():
            try:
                conn.close()
            except Exception:
                pass

if __name__ == "__main__":
    # For development only. In production, run under a WSGI server with TLS termination.
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 5000)))
