from flask import Flask, request, jsonify
import re
import os
import mysql.connector
from mysql.connector import errors as mysql_errors
from werkzeug.security import generate_password_hash

app = Flask(__name__)

# ...existing code...

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Basic request size guard
    max_content_length = 16 * 1024  # 16 KB
    if request.content_length is not None and request.content_length > max_content_length:
        return jsonify({'error': 'Request too large'}), 413

    # Safely obtain and sanitize inputs
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    # Input validation
    if not username or not password:
        return jsonify({'error': 'Missing username or password'}), 400

    # Enforce reasonable length limits to prevent overflows and abuse
    if len(username) < 3 or len(username) > 32:
        return jsonify({'error': 'Username must be 3-32 characters'}), 400
    if len(password) < 12 or len(password) > 128:
        return jsonify({'error': 'Password must be 12-128 characters'}), 400

    # Allow only a safe subset of characters for username
    if not re.fullmatch(r'[A-Za-z0-9_.-]+', username):
        return jsonify({'error': 'Username contains invalid characters'}), 400

    # Do NOT echo back raw user input to avoid XSS; create password hash securely
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Read DB credentials from environment variables (do not hardcode secrets)
    db_config = {
        'host': os.environ.get('DB_HOST', '127.0.0.1'),
        'user': os.environ.get('DB_USER'),
        'password': os.environ.get('DB_PASSWORD'),
        'database': os.environ.get('DB_NAME'),
        'port': int(os.environ.get('DB_PORT', '3306')),
        'charset': 'utf8mb4',
        'use_unicode': True,
    }
    if not db_config['user'] or not db_config['password'] or not db_config['database']:
        # Avoid leaking details about credentials
        return jsonify({'error': 'Server configuration error'}), 500

    # Insert user using parameterized query to prevent SQL injection
    try:
        conn = mysql.connector.connect(**db_config)
        try:
            cursor = conn.cursor(prepared=True)
            insert_sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
            cursor.execute(insert_sql, (username, password_hash))
            conn.commit()
        finally:
            cursor.close()
            conn.close()
    except mysql_errors.IntegrityError:
        # Likely duplicate username; do not reveal details beyond necessary
        return jsonify({'error': 'Username already exists'}), 409
    except Exception:
        # Generic error message; do not leak internal details or stack traces
        return jsonify({'error': 'Internal server error'}), 500

    # Successful creation: do not return secrets or raw inputs
    return jsonify({'status': 'user created'}), 201

# ...existing code...
