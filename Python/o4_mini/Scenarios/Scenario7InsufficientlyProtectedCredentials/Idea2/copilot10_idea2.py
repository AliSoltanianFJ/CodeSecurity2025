from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    import re
    from werkzeug.security import generate_password_hash

    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Validate input
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        return 'Invalid username', 400
    if not (8 <= len(password) <= 128):
        return 'Invalid password length', 400

    # Generate salted hash using PBKDF2
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Store user in the database
    db = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'appuser'),
        passwd=os.environ.get('DB_PASS', 'appsecret'),
        db=os.environ.get('DB_NAME', 'appdb'),
        charset='utf8mb4',
        use_unicode=True
    )
    try:
        cursor = db.cursor()
        insert_sql = 'INSERT INTO users (username, password_hash) VALUES (%s, %s)'
        cursor.execute(insert_sql, (username, password_hash))
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        return 'Username already exists', 409
    except Exception:
        db.rollback()
        return 'Internal Server Error', 500
    finally:
        cursor.close()
        db.close()

    return redirect('/login', code=302)
# ...existing code...