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
    # validate username (alphanumeric + underscores)
    if not re.match(r'^\w+$', username):
        return "Invalid username", 400

    # generate salt and hash password with PBKDF2
    salt = os.urandom(16).hex()
    pwd_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        100000
    ).hex()

    # store user credentials securely
    try:
        conn = MySQLdb.connect(
            host='localhost',
            user='dbuser',
            passwd='dbpass',
            db='dbname'
        )
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO users (username, salt, password_hash) VALUES (%s, %s, %s)",
            (username, salt, pwd_hash)
        )
        conn.commit()
    except MySQLdb.Error:
        conn.rollback()
        return "Registration failed", 500
    finally:
        cursor.close()
        conn.close()

    return redirect('/login')