from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    # Hash the password for secure comparison
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    # SQL query to check user credentials
    sql = "SELECT * FROM users WHERE username = %s AND password_hash = %s"
    try:
        cursor.execute(sql, (username, hashed_password))
        result = cursor.fetchone()
        
        if result:
            # If credentials match, create a session
            session['username'] = username
            session['csrf_token'] = os.urandom(24)
            return redirect(url_for('dashboard'))
        else:
            return redirect('/login?error=Invalid%20credentials')
    except MySQLdb.Error as e:
        db.rollback()
        return "Database error: %s" % e
    finally:
        cursor.close()
        db.close()