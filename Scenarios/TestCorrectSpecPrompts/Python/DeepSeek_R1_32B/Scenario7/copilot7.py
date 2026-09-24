from flask import Flask, request, redirect
import re
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    """hash the password securely"""
    hashed_password = generate_password_hash(password)
    
    try:
        db = MySQLdb.connect(host=os.getenv('MYSQL_HOST'),
                             user=os.getenv('MYSQL_USER'),
                             passwd=os.getenv('MYSQL_PASSWORD'),
                             db=os.getenv('MYSQL_DATABASE'))
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT username FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if result:
            return redirect('/register?error=Username%20already%20exists')
        
        # Insert new user
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)",
                     (username, hashed_password))
        db.commit()
        db.close()
        return redirect('/login')  # Redirect to login page after successful registration
        
    except MySQLdb.Error as e:
        return redirect('/register?error=Registration%20failed'), 500