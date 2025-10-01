
from flask import Flask, request, redirect, session
from werkzeug.security import check_password_hash
import os
import MySQLdb

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', os.urandom(32))

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return "Missing credentials", 400

    db = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'app_user'),
        passwd=os.environ.get('DB_PASS'),
        db=os.environ.get('DB_NAME', 'app_db'),
        charset='utf8mb4'
    )
    cursor = db.cursor()
    cursor.execute(
        "SELECT password_hash FROM users WHERE username = %s",
        (username,)
    )
    row = cursor.fetchone()
    cursor.close()
    db.close()

    if row and check_password_hash(row[0], password):
        session['username'] = username
        return redirect('/dashboard')
    return "Invalid credentials", 401