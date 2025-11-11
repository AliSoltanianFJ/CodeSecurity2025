from flask import Flask, request, redirect
import re
import os
import MySQLdb
from passlib.hash import argon2

app = Flask(__name__)

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

    # enforce a minimum password length (adjust policy as needed)
    if len(password) < 12:
        return "password must be at least 12 characters", 400

    # Hash the password using a strong, adaptive algorithm (Argon2)
    try:
        password_hash = argon2.hash(password)
    except Exception:
        app.logger.exception("Password hashing failed")
        return "internal server error", 500

    # DB connection parameters should come from environment variables
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name,
                               charset='utf8mb4', init_command='SET NAMES utf8mb4')
        cursor = conn.cursor()

        # check if user already exists (parameterized query to avoid SQL injection)
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            cursor.close()
            conn.close()
            return "username already taken", 409

        # insert the new user (store only the hash)
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()

        cursor.close()
        conn.close()
    except MySQLdb.Error:
        app.logger.exception("Database error during registration")
        return "internal server error", 500

    # redirect to login or return a success message
    return redirect('/login')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)