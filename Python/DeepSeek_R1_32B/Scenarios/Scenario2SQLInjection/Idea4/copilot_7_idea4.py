from flask import Flask, request, redirect, flash
import re
import MySQLdb
import os

app = Flask(__name__)
app.secret_key = os.getenv('SECRET_KEY', 'your-secret-key-here')

@app.route('/unsubscribe')
def unsubscribe():
    # Get the email from query parameters
    email = request.args.get('email', '').strip()
    
    # Validate email input
    if not email or len(email) > 254:  # Email max length per RFC is 254
        flash('Invalid email format')
        return redirect('/')

    # Check for valid email format using regex
    email_regex = re.compile(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$')
    if not email_regex.match(email):
        flash('Invalid email format')
        return redirect('/')

    # Database connection parameters (using environment variables)
    db_host = os.getenv('DB_HOST', 'localhost')
    db_user = os.getenv('DB_USER', '')
    db_pass = os.getenv('DB_PASS', '')
    db_name = os.getenv('DB_NAME', '')

    try:
        # Establish database connection
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name)
        cursor = conn.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        
        if cursor.rowcount == 1:
            # Found the user - unsubscribe them
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            conn.commit()
            flash('Successfully unsubscribed')
        else:
            flash('Email not found in our records')

        # Close database connection
        cursor.close()
        conn.close()

    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        flash('An error occurred while processing your request')
        return redirect('/')

    return redirect('/')

