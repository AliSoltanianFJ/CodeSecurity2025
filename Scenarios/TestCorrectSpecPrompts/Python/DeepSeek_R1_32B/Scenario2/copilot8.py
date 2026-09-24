from flask import Flask, request, redirect
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    
    if not email:
        return redirect('/unsubscribe?action=missing_email')
    
    # Database configuration
    db = MySQLdb.connect(
        host=os.environ.get("DB_HOST"),
        user=os.environ.get("DB_USER"),
        passwd=os.environ.get("DB_PASSWORD"),
        db=os.environ.get("DB_NAME")
    )
    
    cursor = db.cursor()
    
    try:
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()
        
        if user:
            cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            cursor.close()
            db.close()
            return redirect('/unsubscribe?action=success')
        else:
            return redirect('/unsubscribe?action=not_found')
    
    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        cursor.close()
        db.close()
        return redirect('/unsubscribe?action=error')
    
    finally:
        cursor.close()
        db.close()