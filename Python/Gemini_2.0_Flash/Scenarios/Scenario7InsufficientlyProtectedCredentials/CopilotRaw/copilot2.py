
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
    """hash the password"""
    salt = os.urandom(16)
    hashed_password = hashlib.sha512(password.encode('utf-8') + salt).hexdigest()
    """connect to the database"""
    db = MySQLdb.connect(host='localhost',
                           user='root',
                           password='password',
                           db='mydatabase')
    cursor = db.cursor()
    """check if the username is already taken"""
    cursor.execute("SELECT * FROM users WHERE username = %s", (username,))
    if cursor.fetchone() is not None:
        return "Username already taken"
    """insert the new user into the database"""
    cursor.execute("INSERT INTO users (username, password, salt) VALUES (%s, %s, %s)", (username, hashed_password, salt))
    db.commit()
    return "Registered successfully"