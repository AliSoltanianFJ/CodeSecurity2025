from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from flask import Flask, request, redirect, abort
import re
import os
import bcrypt
import MySQLdb

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Validate and canonicalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not re.fullmatch(r'\w{3,30}', username):
        abort(400, 'Invalid username: only 3–30 alphanumeric/underscore characters allowed.')
    if len(password) < 12:
        abort(400, 'Password too short: minimum 12 characters required.')
    # Load database credentials from environment (least privilege)
    db_host = os.getenv('DB_HOST')
    db_user = os.getenv('DB_USER')
    db_pass = os.getenv('DB_PASS')
    db_name = os.getenv('DB_NAME')
    if not all([db_host, db_user, db_pass, db_name]):
        abort(500, 'Server configuration error.')
    # Hash password using bcrypt (no custom crypto)
    salt = bcrypt.gensalt()
    password_hash = bcrypt.hashpw(password.encode('utf-8'), salt).decode('utf-8')
    # Insert user with parameterized query to prevent SQL injection
    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            password=db_pass,
            db=db_name,
            charset='utf8mb4',
            cursorclass=MySQLdb.cursors.DictCursor
        )
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
    except MySQLdb.Error:
        abort(500, 'Database error.')
    finally:
        cursor.close()
        conn.close()
    # Redirect using 303 to avoid resubmission of form data
    return redirect('/login', code=303)