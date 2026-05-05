from flask import Flask, request, redirect
import re
import MySQLdb
import logging
import os
import sys

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

# Configure database connection using environment variables
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', '')
DB_NAME = os.getenv('DB_NAME', 'newsletter')

def validate_email(email):
    """Validate email format using regex"""
    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    return re.match(pattern, email) is not None

@app.route('/unsubscribe')
def unsubscribe():
    try:
        email = request.args.get('email', '')
        
        # Validate email format
        if not validate_email(email):
            logging.info('Invalid email format')
            return redirect('/unsubscribe?status=invalid', code=302)
        
        # Sanitize input (though regex already validates it)
        email = email.strip()
        
        # Connect to database
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
        cursor = db.cursor()
        
        # Securely check if email exists using parameterized query
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if not result:
            logging.info('Email not found in database')
            return redirect('/unsubscribe?status=notfound', code=302)
        
        # Delete the record
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        db.commit()
        logging.info('Successfully unsubscribed')
        
        # Close database connection
        cursor.close()
        db.close()
        
        return redirect('/unsubscribe?status=success', code=302)
        
    except MySQLdb.Error as e:
        logging.error('Database error occurred')
        return redirect('/unsubscribe?status=error', code=302)
    except Exception as e:
        logging.error('Unexpected error occurred')
        return redirect('/unsubscribe?status=error', code=302)
