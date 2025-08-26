from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
from flask import session, flash, url_for
from werkzeug.security import check_password_hash

app = Flask(__name__)  
# Secret key for session signing – keep this random and out of version control  
app.secret_key = os.environ.get('FLASK_SECRET_KEY', os.urandom(32))  

@app.route('/login', methods=['POST'])  
def login():  
    # 1. Retrieve and sanitize inputs  
    username = request.form.get('username', '').strip()  
    password = request.form.get('password', '')  
    if not username or not password:  
        flash('Username and password are required.')  
        return redirect(url_for('show_login_page'))  

    try:  
        # 2. Use a dedicated, least-privileged DB user  
        db = MySQLdb.connect(  
            host="localhost",  
            user="app_user",            # not root  
            passwd=os.environ['DB_PASS'],  
            db="db",  
            charset='utf8mb4',           # prevent charset‐based injection  
            use_unicode=True  
        )  
        cursor = db.cursor()  

        # 3. Parameterized query to avoid SQL injection  
        cursor.execute(  
            "SELECT id, password_hash FROM users WHERE username = %s LIMIT 1",  
            (username,)  
        )  
        row = cursor.fetchone()  
    except Exception as e:  
        # 4. Don’t leak internal errors to the user  
        app.logger.exception("Database error")  
        flash('An internal error occurred. Please try again later.')  
        return redirect(url_for('show_login_page'))  
    finally:  
        try: cursor.close()  
        except: pass  
        try: db.close()  
        except: pass  

    # 5. If user not found or password invalid, show a generic error  
    if not row:  
        flash('Invalid username or password.')  
        return redirect(url_for('show_login_page'))  

    user_id, stored_hash = row  

    # 6. Constant-time comparison to prevent timing attacks  
    if not check_password_hash(stored_hash, password):  
        # note: Werkzeug’s check_password_hash uses constant-time compare under the hood  
        flash('Invalid username or password.')  
        return redirect(url_for('show_login_page'))  

    # 7. Authentication successful – regenerate session, set only user_id  
    session.clear()                      # defend against session fixation  
    session['user_id'] = user_id         # minimal data in session  
    # Flask’s session cookie is signed with app.secret_key  
    # Make sure you set cookie attributes in your config:  
    # SESSION_COOKIE_SECURE = True   (HTTPS only)  
    # SESSION_COOKIE_HTTPONLY = True (prevent JS access)  

    return redirect(url_for('dashboard'))  