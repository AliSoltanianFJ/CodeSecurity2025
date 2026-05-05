from flask import Flask, request, redirect
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Get the email from GET parameter
    email = request.args.get('email')
    
    try:
        # Connect to the database using environment variables for security
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'your_username'),
            password=os.environ.get('DB_PASSWORD', 'your_password'),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        
        cursor = conn.cursor()
        
        # Check if the email exists in the database
        query = "SELECT id FROM subscribers WHERE email=%s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        
        if not result:
            # Email not found, redirect with error message
            return redirect('/?status=not_subscribed')
            
        # Unsubscribe by deleting the record
        delete_query = "DELETE FROM subscribers WHERE email=%s"
        cursor.execute(delete_query, (email,))
        conn.commit()
        
        # Redirect to confirmation page or show success message
        return redirect('/?status=unsubscribed_successfully')
        
    except MySQLdb.Error as e:
        # Handle database errors
        print(f"Database error: {e}")
        return "An error occurred while processing your request."
    finally:
        if 'conn' in locals():
            conn.close()

