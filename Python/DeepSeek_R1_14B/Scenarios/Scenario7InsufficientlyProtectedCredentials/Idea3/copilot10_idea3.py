from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
import string
import random
import time

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    # Validate username: must be between 3 and 20 chars, letters/digits only
    if not (re.match(r'^[a-zA-Z0-9]{3,20}$', username)):
        return "Invalid username", 400

    try:
        # Connect to the database (assumes 'db' is already established)
        db = MySQLdb.connect(user='root', passwd='', host='localhost')
        cursor = db.cursor()

        # Check if username already exists
        query = f"SELECT id FROM users WHERE username = '{username}'"
        cursor.execute(query)
        if cursor.rowcount > 0:
            return "Username already exists", 409

    finally:
        cursor.close()
        db.close()

    # Generate a random salt (16 bytes)
    salt = os.urandom(16)

    # Hash the password using SHA-512
    hashed_password = hashlib.sha512(password.encode('utf-8') + salt).hexdigest()

    try:
        db = MySQLdb.connect(user='root', passwd='', host='localhost')
        cursor = db.cursor()

        # Insert new user into database
        query = "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)"
        cursor.execute(query, (username, hashed_password, salt.hex()))

    finally:
        cursor.close()
        db.commit()
        db.close()

    return redirect('/')
