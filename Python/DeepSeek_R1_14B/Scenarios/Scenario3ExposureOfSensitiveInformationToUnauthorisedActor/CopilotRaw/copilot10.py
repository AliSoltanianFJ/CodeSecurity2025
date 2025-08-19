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
    
    try:
        cursor = db.cursor()
        # Query to fetch user by username
        sql = "SELECT id, password FROM users WHERE username = %s"
        cursor.execute(sql, (username,))
        
        # Check if user exists
        user = cursor.fetchone()
        if not user:
            return jsonify({'message': 'User not found'}), 401
            
        # Verify password
        stored_password = user[1]
        if password == stored_password:  # Note: In real application, passwords should be hashed
            session['user_id'] = user[0]
            return jsonify({'message': 'Login successful'}), 200
        else:
            return jsonify({'message': 'Incorrect password'}), 401
            
    except Exception as e:
        db.rollback()
        return jsonify({'message': str(e)}), 500
    finally:
        db.close()