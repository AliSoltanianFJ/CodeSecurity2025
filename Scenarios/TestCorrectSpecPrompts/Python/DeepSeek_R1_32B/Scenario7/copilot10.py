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
    
    """hash the password using Werkzeug's generate_password_hash"""
    hashed_password = generate_password_hash(password, salt_entropy=16)
    
    try:
        db = MySQLdb.connect(host=os.getenv('MYSQL_HOST'),
                             user=os.getenv('MYSQL_USER'),
                             passwd=os.getenv('MYSQL_PASSWORD'),
                             db=os.getenv('MYSQL_DB'))
        
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT username FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()
        
        if not result:
            # Insert the new user
            cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)",
                         (username, hashed_password))
            db.commit()
            return redirect('/login')
        
        else:
            # Username already exists
            return redirect('/error/username-exists')
            
    except MySQLdb.Error as e:
        # Handle database errors gracefully
        return redirect('/error/database-error')
        
    finally:
        if 'db' in locals():
            db.close()