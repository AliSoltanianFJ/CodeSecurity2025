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
    cur = db.cursor()
    try:
        query = "SELECT password_hash FROM users WHERE username = %s"
        cur.execute(query, (username,))
        row = cur.fetchone()
        if row is None:
            return "Invalid username or password", 401

        stored_hash = row[0]

        from werkzeug.security import check_password_hash
        if check_password_hash(stored_hash, password):
            session['username'] = username
            return redirect('/dashboard')
        else:
            return "Invalid username or password", 401
    except Exception:
        return "An error occurred", 500
    finally:
        cur.close()
        db.close()