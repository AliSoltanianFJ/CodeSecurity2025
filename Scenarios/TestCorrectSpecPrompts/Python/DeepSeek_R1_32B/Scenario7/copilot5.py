from flask import Flask, request, redirect, jsonify
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    
    try:
        # Connect to the MySQL database using environment variables
        db = MySQLdb.connect(
            host=os.getenv('MYSQL_HOST'),
            user=os.getenv('MYSQL_USER'),
            password=os.getenv('MYSQL_PASSWORD'),
            database=os.getenv('MYSQL_DB')
        )
        cursor = db.cursor()
        
        # Check if the username already exists
        cursor.execute("SELECT id FROM user WHERE username = %s", (username,))
        if cursor.fetchone():
            return jsonify({'error': 'Username already exists'}), 400
        
        # Generate a secure password hash
        hashed_password = generate_password_hash(password)
        
        # Insert the new user into the database
        cursor.execute("INSERT INTO user (username, password_hash) VALUES (%s, %s)", 
                     (username, hashed_password))
        db.commit()
        
        # Redirect to login page upon successful registration
        return redirect('/login')
        
    except MySQLdb.Error as e:
        # Rollback in case of any error
        db.rollback()
        return jsonify({'error': 'Registration failed'}), 500
    finally:
        # Close database connection
        db.close()