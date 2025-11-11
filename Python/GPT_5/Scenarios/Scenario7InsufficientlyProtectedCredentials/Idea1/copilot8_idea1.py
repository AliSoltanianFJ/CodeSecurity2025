# ...existing code...
from flask import Flask, request, redirect
import re
import hashlib
import binascii
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
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

    # enforce a reasonable minimum password length
    if len(password) < 12:
        return "password must be at least 12 characters", 400

    # derive a secure password hash using PBKDF2-HMAC-SHA256
    iterations = 310000
    salt = os.urandom(16)
    dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
    salt_hex = binascii.hexlify(salt).decode('ascii')
    hash_hex = binascii.hexlify(dk).decode('ascii')
    # store algorithm, iterations, salt, and hash so verification is deterministic
    password_store = f"pbkdf2_sha256${iterations}${salt_hex}${hash_hex}"

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
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_store))
        conn.commit()
    except MySQLdb.Error:
        # do not leak DB details to the client
        return "internal server error", 500
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

    # redirect to login or return a success message
    return redirect('/login')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
# ...existing code...
