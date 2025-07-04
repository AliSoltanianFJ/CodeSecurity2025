from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from werkzeug.security import check_password_hash

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    if not username or not password:
        return "Missing credentials", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    except Exception as db_err:
        return "Database connection error", 500

    try:
        cursor = db.cursor()
        # Use a parameterized query to prevent SQL injection.
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]
    finally:
        cursor.close()
        db.close()

    # Verify the provided password against the stored hash.
    if check_password_hash(stored_hash, password):
        return redirect("/dashboard")
    else:
        return "Invalid credentials", 401
