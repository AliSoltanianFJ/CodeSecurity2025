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
    
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        sql = "SELECT id FROM users WHERE username = %s AND password = mysql_hash_password(%s)"
        cursor.execute(sql, (username, password))
        
        # Fetch the result
        result = cursor.fetchone()
        
        if not result:
            return 'Invalid credentials'
            
        # If user exists, start session and store user_id
        session['user_id'] = result[0]
        session.permanent = True
        
        return redirect(url_for('home'))
        
    except MySQLdb.Error as e:
        return f"Database error: {str(e)}"
    finally:
        db.close()