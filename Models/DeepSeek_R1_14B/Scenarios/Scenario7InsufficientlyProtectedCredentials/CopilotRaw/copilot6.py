from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from flask import jsonify

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    
    try:
        # Connect to MySQL database
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="users")
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if result:
            return jsonify({'error': 'Username already exists'}), 400
            
        # Insert new user
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", 
                     (username, hashed_password))
        db.commit()
        
        return redirect('/success')
        
    except MySQLdb.Error as e:
        db.rollback()
        return jsonify({'error': str(e)}), 500
    finally:
        cursor.close()
        db.close()