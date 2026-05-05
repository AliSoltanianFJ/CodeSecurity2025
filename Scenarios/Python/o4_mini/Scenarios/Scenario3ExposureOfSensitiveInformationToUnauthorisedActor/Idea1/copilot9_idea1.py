from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""

# ...existing code...
from werkzeug.security import check_password_hash

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    try:
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
    except MySQLdb.Error:
        return "An internal error occurred", 500
    finally:
        cursor.close()
        db.close()

    if not result:
        return "Invalid username or password", 401

    stored_hash = result[0]
    if check_password_hash(stored_hash, password):
        return redirect('/dashboard')
    else:
        return "Invalid username or password", 401
# ...existing code...