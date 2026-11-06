from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)


@app.route('/register', methods=['POST'])
def register():
    # 1. Retrieve and validate input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        return "Username and password are required.", 400
    # Username: 3–30 chars, letters, digits, dot, underscore, hyphen
    if not re.fullmatch(r'[A-Za-z0-9_.-]{3,30}', username):
        return "Invalid username.", 400
    # Password strength: minimum length
    if len(password) < 12:
        return "Password must be at least 12 characters long.", 400

    # 2. Hash the password with bcrypt + optional pepper
    try:
        pepper = os.getenv('PASSWORD_PEPPER', '')
        password_bytes = (password + pepper).encode('utf-8')
        # bcrypt.gensalt defaults to 12 rounds; adjust via BCRYPT_ROUNDS env if needed
        rounds = int(os.getenv('BCRYPT_ROUNDS', '12'))
        salt = bcrypt.gensalt(rounds=rounds)
        password_hash = bcrypt.hashpw(password_bytes, salt).decode('utf-8')
    except Exception as e:
        app.logger.error("Password hashing failed: %s", e)
        return "Registration failed.", 500

    # 3. Insert into database using parameterized queries
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
        # users.username should be UNIQUE in the schema
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        # Likely a duplicate username; do not reveal details
        app.logger.info("Duplicate registration attempt for username '%s'", username)
        return "Registration failed.", 400
    except Exception as e:
        app.logger.error("Database error during registration for '%s': %s", username, e)
        return "Registration failed.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    # 4. On success, redirect to login using a 303 to enforce GET
    return redirect('/login', code=303)
