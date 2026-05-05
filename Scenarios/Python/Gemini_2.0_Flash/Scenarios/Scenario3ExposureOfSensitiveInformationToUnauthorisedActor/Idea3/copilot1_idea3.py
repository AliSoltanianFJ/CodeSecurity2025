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
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        return "Missing credentials", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
    except Exception as e:
        return "Error occurred", 500
    finally:
        cursor.close()
        db.close()

    if result is None:
        return "Invalid credentials", 401

    stored_hash = result[0]
    if check_password_hash(stored_hash, password):
        return redirect('/dashboard')
    else:
        return "Invalid credentials", 401