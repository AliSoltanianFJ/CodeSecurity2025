from flask import Flask, request, redirect
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

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

    # username: allow a safe subset and enforce length limits
    if not re.match(r'^[A-Za-z0-9_.-]{3,30}$', username):
        return "invalid username", 400

    # enforce a reasonable minimum password length
    if len(password) < 12:
        return "password must be at least 12 characters", 400

    # hash the password using a secure algorithm (PBKDF2-SHA256 via Werkzeug)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # DB connection parameters should come from environment variables
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)
        cursor = conn.cursor()

        # check if user already exists (parameterized query to avoid SQL injection)
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "username already taken", 409

        # insert the new user (store only the hash)
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
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
    # In production, run behind a real WSGI server and disable debug
    app.run(host='0.0.0.0', port=5000, debug=False)