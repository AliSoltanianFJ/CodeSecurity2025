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
    # validate username
    if not re.match(r'^\w{3,20}$', username):
        return 'Invalid username', 400
    # salt and hash the password using PBKDF2
    salt = os.urandom(16).hex()
    hashed = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt.encode('utf-8'), 100000).hex()
    # store credentials
    conn = MySQLdb.connect(host='localhost', user='appuser', passwd='apppass', db='appdb')
    cursor = conn.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, salt, password_hash) VALUES (%s, %s, %s)",
            (username, salt, hashed)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        conn.rollback()
        return 'Username already exists', 409
    finally:
        cursor.close()
        conn.close()
    return redirect('/login')
# ...existing code...