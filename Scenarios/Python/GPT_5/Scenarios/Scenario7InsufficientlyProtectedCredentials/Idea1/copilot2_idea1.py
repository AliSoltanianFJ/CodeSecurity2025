from flask import Flask, request, redirect, jsonify
import re
import os
import logging
import MySQLdb
from argon2 import PasswordHasher

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# use Argon2 for password hashing
ph = PasswordHasher(time_cost=2, memory_cost=102400, parallelism=8)

@app.route('/register', methods=['POST'])
def register():
    # get form values safely
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic input validation
    if not username or not password:
        return "username and password are required", 400

    # enforce username length and allowed characters
    if not re.match(r'^[A-Za-z0-9_.-]{3,30}$', username):
        return "invalid username", 400

    # enforce a reasonable minimum password length
    if len(password) < 12:
        return "password must be at least 12 characters", 400

    # hash the password using Argon2 (resistant to GPU attacks)
    try:
        password_hash = ph.hash(password)
    except Exception:
        logging.exception("Password hashing failed")
        return "internal server error", 500

    # DB connection parameters should come from environment variables
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')
    db_port = int(os.environ.get('DB_PORT', '3306'))

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, port=db_port, charset='utf8mb4')
        cursor = conn.cursor()

        # parameterized query to avoid SQL injection
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            # username already exists
            cursor.close()
            conn.close()
            return "username unavailable", 409

        # store only the hashed password
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

    # successful registration - redirect to login
    return redirect('/login')

if __name__ == '__main__':
    # In production, run behind a real WSGI server and disable debug
    app.run(host='0.0.0.0', port=5000, debug=False)
