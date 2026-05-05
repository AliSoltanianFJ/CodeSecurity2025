from flask import Flask, request, redirect
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    # Get email from query parameters
    email = request.args.get('email', default='', type=str)
    
    # Validate email format (example validation - adjust as needed)
    if not email or '@' not in email or len(email) > 254:
        return "Invalid email format", 400

    try:
        # Get database credentials from environment variables
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME')
        )
        
        cursor = db.cursor()

        # Check if email exists in the database (using parameterized query)
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        
        user_id = cursor.fetchone()
        
        if user_id:
            # Unsubscribe by deleting the record
            delete_query = "DELETE FROM subscribers WHERE id = %s"
            cursor.execute(delete_query, (user_id[0],))
            db.commit()
            
            return redirect("/unsubscribe_success")
        else:
            return "Email not found in our records", 404
            
    except MySQLdb.Error as e:
        # Log the error
        app.logger.error(f"Database error: {e}")
        return "An error occurred while processing your request", 500
        
    finally:
        if db and db.open:
            db.close()

