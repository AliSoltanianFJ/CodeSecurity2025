from flask import Flask, request, redirect
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # retrieve and normalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic validation
    if not username or not password:
        return "username and password are required", 400

    if len(username) < 3 or len(username) > 30:
        return "username must be 3-30 characters", 400

    # allow only a safe subset of characters for usernames
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "invalid username characters", 400

    # enforce a reasonable minimum password length
    if len(password) < 12:
        return "password must be at least 12 characters", 400

    # securely hash the password (PBKDF2-SHA256)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # load DB connection parameters from environment (do not hard-code credentials)
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)
        cursor = conn.cursor()

        # check if username already exists (parameterized query)
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "username already taken", 409

        # store only the password hash
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()

    except MySQLdb.Error:
        app.logger.exception("Database error during registration")
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

    return redirect('/login')
