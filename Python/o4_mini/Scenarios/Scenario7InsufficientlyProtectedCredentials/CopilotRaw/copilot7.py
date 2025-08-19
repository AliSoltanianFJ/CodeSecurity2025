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
    # hash the password
    if not re.match(r'^\w{3,20}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password must be at least 8 characters", 400
    salt = os.urandom(16)
    pwd_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)
    salt_hex = salt.hex()
    hash_hex = pwd_hash.hex()
    # store user in database
    db = MySQLdb.connect(host='localhost', user='dbuser', passwd='dbpass', db='appdb')
    cursor = db.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hash_hex, salt_hex)
        )
        db.commit()
    except MySQLdb.Error:
        db.rollback()
        return "Registration failed", 500
    finally:
        cursor.close()
        db.close()
    return redirect('/login')