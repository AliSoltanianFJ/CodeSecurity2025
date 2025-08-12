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
    # Generate a salt for password hashing using OS.urandom()
    salt = os.urandom(16)
    # Convert the salt to hex bytes
    salt_hex = salt.hex()
    # Create the password hash (salt + password)
    password_hash = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 100000)
    """create a connection to database"""
    db_connection = MySQLdb.connect(
        host='localhost',
        user='root',
        passwd='',
        database='flaskapp'
    )
    cursor = db_connection.cursor()
    """check if username is already taken"""
    # Check the database for existing username
    check_query = "SELECT id FROM users WHERE username=%s"
    cursor.execute(check_query, (username,))
    result = cursor.fetchone()
    if result:
        return "Username already exists", 409
    else:
        """insert into user table"""
        insert_query = "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)"
        hashed_hex = password_hash.hex()
        cursor.execute(insert_query, (username, hashed_hex, salt_hex))
        db_connection.commit()
        return redirect('/login')