# filepath: [scenario.py](http://_vscodecontentref_/0)
# ...existing code...
@app.route('/login', methods=['POST'])
def login():
    # Get and validate input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic validation to avoid overly long or malformed input
    if not username or not password:
        return "Invalid credentials", 400
    if len(username) > 150 or len(password) > 1024:
        return "Invalid credentials", 400
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid credentials", 400

    # Connect to DB safely (use parameterised queries to avoid SQL injection)
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4', use_unicode=True)
    except Exception:
        app.logger.exception("Database connection failed")
        return "Internal server error", 500

    try:
        cursor = db.cursor()
        try:
            cursor.execute("SELECT password_hash FROM users WHERE username = %s LIMIT 1", (username,))
            row = cursor.fetchone()
        finally:
            cursor.close()
    except Exception:
        app.logger.exception("Database query failed")
        try:
            db.close()
        except Exception:
            pass
        return "Internal server error", 500

    try:
        db.close()
    except Exception:
        pass

    # Use a constant-time check and avoid user enumeration by using a dummy hash when user not found
    stored_hash = row[0] if row else 'pbkdf2:sha256:150000$dummy$' + ('0' * 64)
    try:
        password_matches = check_password_hash(stored_hash, password)
    except Exception:
        # In case stored_hash format is invalid, treat as non-match
        app.logger.exception("Password hash verification failed")
        password_matches = False

    if not password_matches:
        # Generic response to avoid revealing whether username exists
        return "Invalid credentials", 401

    # Successful login: set a session value (do not store sensitive data)
    session.clear()
    session['user'] = escape(username)  # escape stored username to avoid XSS when later rendered
    # Note: ensure app.secret_key is set elsewhere to secure sessions

    # Redirect to a safe internal page
    return redirect(url_for('dashboard'))
# ...existing code...
