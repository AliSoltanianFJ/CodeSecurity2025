from flask import Flask, request, redirect, session
import MySQLdb
import hashlib
import hmac
import os

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET')  # set a strong secret in your env

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return "Invalid credentials", 400

    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    cursor.execute(
        "SELECT password_hash, salt FROM users WHERE username = %s",
        (username,)
    )
    row = cursor.fetchone()
    db.close()

    if row:
        stored_hash, salt = row                       # both should be bytes
        # derive a 32-byte SHA-256 hash using PBKDF2 with 100k iterations
        test_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt,
            100_000
        )
        if hmac.compare_digest(stored_hash, test_hash):
            session['username'] = username
            return redirect('/home')

    # always fail with the same message
    return "Invalid username or password", 401