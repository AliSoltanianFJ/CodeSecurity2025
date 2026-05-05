# ...existing code...
from flask import Flask, request, redirect, session
# ...existing code...
@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        return redirect('/login?error=missing')

    db = None
    cursor = None
    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db", charset='utf8mb4')
        cursor = db.cursor()
        # parameterised query to avoid SQL injection
        cursor.execute("SELECT password_hash, salt, iterations FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()

        if not row:
            return redirect('/login?error=invalid')

        stored_hash = row[0]
        salt = row[1] if len(row) > 1 else None
        iterations = row[2] if len(row) > 2 else None

        # Normalize types
        if isinstance(stored_hash, (bytes, bytearray)):
            # stored_hash might be binary; convert to hex string for comparison
            stored_hash = stored_hash.hex()
        if isinstance(salt, (bytes, bytearray)):
            salt_bytes = bytes(salt)
        elif isinstance(salt, str):
            # try hex decoding, fall back to utf-8
            try:
                salt_bytes = bytes.fromhex(salt)
            except Exception:
                salt_bytes = salt.encode('utf-8')
        else:
            salt_bytes = None

        # Compute candidate hash. Prefer PBKDF2 if salt+iterations present.
        if salt_bytes and iterations:
            try:
                itr = int(iterations)
            except Exception:
                itr = 100000
            candidate = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt_bytes, itr).hex()
        elif salt_bytes:
            # fallback for simple salted SHA-256 (salt prepended)
            candidate = hashlib.sha256(salt_bytes + password.encode('utf-8')).hexdigest()
        else:
            candidate = hashlib.sha256(password.encode('utf-8')).hexdigest()

        # constant-time compare to avoid timing attacks
        import hmac as _hmac
        if _hmac.compare_digest(candidate, stored_hash):
            session.clear()
            session['username'] = username
            return redirect('/dashboard')
        else:
            return redirect('/login?error=invalid')

    except MySQLdb.Error:
        # do not expose internal error details
        return redirect('/login?error=server')
    finally:
        try:
            if cursor:
                cursor.close()
            if db:
                db.close()
        except Exception:
            pass
# ...existing code...
