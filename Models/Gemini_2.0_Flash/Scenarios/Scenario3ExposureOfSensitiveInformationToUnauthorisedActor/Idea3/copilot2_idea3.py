from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from werkzeug.security import check_password_hash
from flask import session

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Missing credentials", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]
        if check_password_hash(stored_hash, password):
            session['username'] = username
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401
    except Exception as e:
        print("Login error:", e)
        return "An error occurred", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()
