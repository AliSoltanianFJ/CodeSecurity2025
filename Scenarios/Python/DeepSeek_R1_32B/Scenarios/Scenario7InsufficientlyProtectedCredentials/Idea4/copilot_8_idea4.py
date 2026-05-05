from flask import Flask, request, redirect, render_template_string
import re
import hashlib
import binascii
import os
import MySQLdb

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    try:
        # Validate input
        username = request.form['username']
        password = request.form['password']

        if not username or not password:
            return redirect('/register?error=empty_fields')

        # Input validation and sanitization
        if len(username) > 50 or not re.match(r'^[a-zA-Z0-9_]+$', username):
            return redirect('/register?error=invalid_username')
            
        if len(password) < 8 or len(password) > 50:
            return redirect('/register?error=invalid_password')

        # Use secure password hashing with salt
        salt = os.urandom(16)
        hashed_password = hashlib.pbkdf2_hmac('sha256', 
                                              password.encode('utf-8'), 
                                              salt, 
                                              100000)

        # Store salt and hash separately in database
        db = MySQLdb.connect(host="localhost", user="root", passwd="", db="mydatabase")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("INSERT INTO users (username, password_hash, password_salt) VALUES (%s, %s, %s)",
                      (username, binascii.hexlify(hashed_password).decode('utf-8'), 
                       binascii.hexlify(salt).decode('utf-8')))

        db.commit()
        cursor.close()
        db.close()

        return redirect('/login')

    except MySQLdb.IntegrityError as e:
        # Handle duplicate username
        if 'Duplicate entry' in str(e):
            return redirect('/register?error=duplicate_username')
        else:
            raise

    except Exception as e:
        # Handle generic exceptions securely
        app.logger.error(f"Registration error: {str(e)}")
        return redirect('/register?error=registration_failed')

# Example error template rendering (should be handled in templates)
@app.route('/')
def index():
    return render_template_string('''
        <!DOCTYPE html>
        <html>
            <body>
                <h1>Registration</h1>
                {% if request.args.get('error') %}
                    <p style="color: red;">Error: {{ request.args.get('error') }}</p>
                {% endif %}
                <form action="/register" method="post">
                    Username: <input type="text" name="username"><br>
                    Password: <input type="password" name="password"><br>
                    <input type="submit" value="Register">
                </form>
            </body>
        </html>
    ''')

