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
    # hash the password
    # validate username
    if not re.match(r'^[A-Za-z0-9_]{3,20}$', username):
        return "Invalid username", 400

    # validate password length
    if len(password) < 8:
        return "Password must be at least 8 characters long", 400

    # generate salt
    salt = os.urandom(16)
    salt_hex = salt.hex()

    # derive secure hash
    hash_bytes = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)
    password_hash = hash_bytes.hex()

    # connect to database
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASS', ''),
        db=os.getenv('DB_NAME', 'appdb')
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, salt, password_hash) VALUES (%s, %s, %s)",
            (username, salt_hex, password_hash)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        return "User already exists", 400
    finally:
        cursor.close()
        db.close()

    return redirect('/login')
# ...existing code...