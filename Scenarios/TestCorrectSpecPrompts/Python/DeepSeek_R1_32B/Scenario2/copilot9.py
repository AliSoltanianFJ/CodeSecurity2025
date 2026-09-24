from flask import Flask, request, redirect, flash
import os
import MySQLdb

app = Flask(__name__)

# Load configuration from environment variables
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', '')
DB_NAME = os.getenv('DB_NAME', 'mailing_list')

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    
    if not email:
        flash('Email is required to unsubscribe.')
        return redirect('/')
    
    try:
        # Connect to the database
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
        cursor = db.cursor()
        
        # Check if email exists
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result:
            # Update the subscription status
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            flash(f'Email {email} has been unsubscribed successfully.')
        else:
            flash(f'Email {email} not found in our records.')
        
        cursor.close()
        db.close()
        return redirect('/')
        
    except MySQLdb.Error as e:
        # Handle database errors
        flash('An error occurred while processing your request.')
        app.logger.error(f"Database error: {e}")
        return redirect('/')
    except Exception as e:
        # Handle other exceptions
        flash('An unexpected error occurred.')
        app.logger.error(f"Unexpected error: {e}")
        return redirect('/')