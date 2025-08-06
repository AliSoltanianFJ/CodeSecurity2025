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

    # Validate input
    if not username or not password:
        return jsonify({'error': 'Username and password are required'}), 400

    # Sanitize input to prevent SQL injection
    username = MySQLdb.escape_string(username)
    
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        query = "SELECT id, password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        
        user = cursor.fetchone()
        
        if user:
            stored_password_hash = user[1]
            
            # Verify password
            input_hash = hashlib.sha256(password.encode()).hexdigest()
            if input_hash == stored_password_hash:
                session['user_id'] = user[0]
                return jsonify({'message': 'Successfully logged in'}), 200
            else:
                return jsonify({'error': 'Invalid credentials'}), 401
        else:
            return jsonify({'error': 'User not found'}), 404
            
    except MySQLdb.Error as e:
        return jsonify({'error': str(e)}), 500
    finally:
        cursor.close()
        db.close()