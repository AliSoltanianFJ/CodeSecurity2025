from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']

    # Connect to the database
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()

    try:
        # Query for the user's data
        query = "SELECT id, salt, password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if not result:
            return "Invalid credentials"

        user_id, stored_salt, stored_hash = result

        # Hash the provided password with the retrieved salt
        hashed_password = hashlib.sha256(stored_salt.encode() + password.encode()).hexdigest()

        if hashed_password == stored_hash:
            # Successful login; set session or other indicators here
            return "Login successful"
        else:
            return "Invalid credentials"

    finally:
        cursor.close()
        db.close()

