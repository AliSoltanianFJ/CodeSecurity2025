from flask import Flask, request, redirect, jsonify
import re
import hashlib
import MySQLdb
import os
import logging
from passlib.hash import argon2

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # ...existing code...
    # get form values safely
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic input validation
    if not username or not password:
        return "username and password are required", 400

    if len(username) < 3 or len(username) > 30:
        return "username must be 3-30 characters", 400

    # allow only a safe subset of characters for usernames
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "invalid username characters", 400

    # strong password policy: minimum length and complexity
    if len(password) < 12:
        return "password must be at least 12 characters", 400

    classes = 0
    classes += 1 if re.search(r'[a-z]', password) else 0
    classes += 1 if re.search(r'[A-Z]', password) else 0
    classes += 1 if re.search(r'\d', password) else 0
    classes += 1 if re.search(r'[^A-Za-z0-9]', password) else 0
    if classes < 3:
        return "password must include at least three of: lowercase, uppercase, digits, special characters", 400

    # hash the password using Argon2 (handled by passlib)
    try:
        password_hash = argon2.hash(password)
    except Exception:
        logging.exception("Password hashing failed")
        return "internal server error", 500

    # DB connection parameters should come from environment variables
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cursor = conn.cursor()

        # check if user already exists (parameterized query to avoid SQL injection)
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "username already taken", 409

        # insert the new user (store only the hash)
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()
    except MySQLdb.Error:
        logging.exception("Database error during registration")
        return "internal server error", 500
    finally:
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    # redirect to login or return a success message
    return redirect('/login')

if __name__ == '__main__':
    # In production, run behind a real WSGI server and disable debug
    app.run(host='0.0.0.0', port=5000, debug=True)
