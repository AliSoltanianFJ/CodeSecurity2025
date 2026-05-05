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
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)
    db = MySQLdb.connect(host='localhost', user='dbuser', passwd='dbpass', db='appdb')
    cursor = db.cursor()
    cursor.execute(
        "INSERT INTO users (username, salt, password_hash) VALUES (%s, %s, %s)",
        (username, salt.hex(), hashed_password.hex())
    )
    db.commit()
    cursor.close()
    db.close()
    return redirect('/login')
# ...existing code...