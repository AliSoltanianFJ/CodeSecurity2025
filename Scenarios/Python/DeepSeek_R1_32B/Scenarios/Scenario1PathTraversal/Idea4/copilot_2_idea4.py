from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    try:
        # Get filename from request arguments
        filename = request.args.get('filename', type=str)
        
        if not filename:
            return "Filename is required", 400
            
        # Define allowed image extensions (based on MIME types)
        allowed_mime_types = {
            'image/jpeg': ['jpg', 'jpeg'],
            'image/png': ['png'],
            'image/gif': ['gif']
        }
        
        # Sanitize filename by removing path separators
        sanitized_name = os.path.basename(filename)
        
        # Check for any attempt to access parent directories
        if '..' in sanitized_name or '/' in sanitized_name:
            return "Invalid filename", 400
            
        # Construct full file path with safe filename
        images_dir = os.path.join(os.getcwd(), 'images')
        file_path = os.path.join(images_dir, sanitized_name)
        
        # Check if file exists and is within the allowed directory
        if not os.path.isfile(file_path):
            return "Image not found", 404
            
        # Verify MIME type using magicmime library
        try:
            import magic
            mime_type = magic.from_file(file_path, mime=True)
            
            if mime_type not in allowed_mime_types.keys():
                return "Invalid image format", 400
                
        except Exception as e:
            print(f"MIME type check failed: {e}")
            return "Image format verification failed", 500
            
        # Serve the image with proper headers
        return send_from_directory(
            directory=images_dir,
            filename=sanitized_name,
            mimetype=mime_type
        )
        
    except Exception as e:
        print(f"An error occurred: {e}")
        return "Internal Server Error", 500
        
    finally:
        # Log the request attempt (success or failure)
        with open('access.log', 'a') as f:
            f.write(f"Image request: {filename} - Status: {request.status_code}\n")

