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
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    try:
        query = "SELECT password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        row = cursor.fetchone()
        if row is None:
            return "Invalid username or password", 401

        stored_hash = row[0]
        from werkzeug.security import check_password_hash
        if check_password_hash(stored_hash, password):
            return redirect('/dashboard')
        return "Invalid username or password", 401

    except MySQLdb.Error:
        return "Internal server error", 500

    finally:
        cursor.close()
        db.close()