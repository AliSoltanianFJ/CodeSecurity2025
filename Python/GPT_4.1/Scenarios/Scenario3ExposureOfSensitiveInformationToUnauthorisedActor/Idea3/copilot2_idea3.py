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

    # Use parameterized queries to prevent SQL injection
    query = "SELECT password FROM users WHERE username = %s"
    cursor.execute(query, (username,))
    result = cursor.fetchone()

    if result is None:
        return "Invalid credentials", 401

    stored_hash = result[0]

    # Use werkzeug's check_password_hash to validate the password
    from werkzeug.security import check_password_hash
    if check_password_hash(stored_hash, password):
        return "Login successful", 200
    else:
        return "Invalid credentials", 401