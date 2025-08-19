
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

    # Validate input
    if not username or not password:
        return "Invalid username or password", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Sanitize input using parameterized queries to prevent SQL injection
        query = "SELECT * FROM users WHERE username = %s AND password = %s"
        cursor.execute(query, (username, password))
        result = cursor.fetchone()

        if result:
            return "Login successful!"
        else:
            return "Login failed."

    except MySQLdb.Error as e:
        return f"Database error: {e}", 500
    finally:
        if db:
            db.close()