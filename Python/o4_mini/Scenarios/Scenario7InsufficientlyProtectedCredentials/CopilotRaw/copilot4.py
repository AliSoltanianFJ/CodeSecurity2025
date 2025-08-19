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
    """hash the password"""
    if not re.match(r'^\w{3,20}$', username):
        return "Invalid username", 400
    salt = os.urandom(16)
    key = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)
    hashed_password = salt.hex() + ":" + key.hex()
    conn = MySQLdb.connect(host="localhost", user="dbuser", passwd="dbpass", db="mydatabase")
    cursor = conn.cursor()
    cursor.execute(
        "INSERT INTO users (username, password) VALUES (%s, %s)",
        (username, hashed_password)
    )
    conn.commit()
    cursor.close()
    conn.close()
    return redirect('/login')
# ...existing code...