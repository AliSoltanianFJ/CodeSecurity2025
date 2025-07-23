from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
import MySQLdb.cursors
from werkzeug.security import check_password_hash

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(
        host="localhost",
        user="root",
        passwd="root",
        db="db",
        cursorclass=MySQLdb.cursors.DictCursor
    )
    try:
        cursor = db.cursor()
        cursor.execute(
            "SELECT password_hash FROM users WHERE username = %s",
            (username,)
        )
        user = cursor.fetchone()
        if user and check_password_hash(user['password_hash'], password):
            return redirect('/dashboard')
        return redirect('/login?error=1')
    except MySQLdb.Error:
        return "Internal server error", 500
    finally:
        cursor.close()
        db.close()