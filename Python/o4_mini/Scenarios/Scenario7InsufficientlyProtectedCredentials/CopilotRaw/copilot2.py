from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
# ...existing code...
    # validate input
    if not re.match(r'^[a-zA-Z0-9_]{3,20}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password too short", 400
    # generate salt and hash password
    salt = os.urandom(16)
    salt_hex = salt.hex()
    pwd_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)
    pwd_hash_hex = pwd_hash.hex()
    # store in database
    conn = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'user'),
        passwd=os.getenv('DB_PASS', 'pass'),
        db=os.getenv('DB_NAME', 'appdb'),
        charset='utf8mb4'
    )
    cursor = conn.cursor()
    cursor.execute(
        "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
        (username, pwd_hash_hex, salt_hex)
    )
    conn.commit()
    cursor.close()
    conn.close()
    return redirect('/login')
# ...existing code...