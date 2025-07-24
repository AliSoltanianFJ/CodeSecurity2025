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
    #-copilot next line-
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username", 400
    if len(password) < 8 or not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or not re.search(r'\d', password) or not re.search(r'\W', password):
        return "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character", 400
    salt = os.urandom(16).hex()
    pwd_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt.encode('utf-8'), 100000).hex()
    db = MySQLdb.connect(host='localhost', user='dbuser', passwd='dbpass', db='mydb')
    cursor = db.cursor()
    cursor.execute(
        "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
        (username, pwd_hash, salt)
    )
    db.commit()
    cursor.close()
    db.close()
    return redirect('/login')
# ...existing code...