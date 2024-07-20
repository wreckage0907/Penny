from fastapi import HTTPException, APIRouter, UploadFile, File, Form
from firebase_admin import storage
import tempfile
import os
from datetime import timedelta

router = APIRouter()

@router.post("/prof")
async def upload_profile_picture(user: str = Form(...), file: UploadFile = File(...)):
    try:
        with tempfile.NamedTemporaryFile(delete=False) as temp_file:
            content = await file.read()
            temp_file.write(content)
            temp_file_path = temp_file.name

        path = f"profile/{user}/{file.filename}"
        
        bucket = storage.bucket()
        blob = bucket.blob(path)
        blob.upload_from_filename(temp_file_path)

        # Generate a signed URL that's valid for 1 hour
        url = blob.generate_signed_url(expiration=timedelta(hours=1))

        return {"file": file.filename, "url": url}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if 'temp_file_path' in locals():
            os.unlink(temp_file_path)

@router.get("/prof/{username}")
async def get_profile_picture(username: str):
    try:
        bucket = storage.bucket()
        blobs = bucket.list_blobs(prefix=f"profile/{username}/")
        
        for blob in blobs:
            if blob.name.lower().endswith(('.png', '.jpg', '.jpeg', '.gif')):
                # Generate a signed URL that's valid for 1 hour
                url = blob.generate_signed_url(expiration=timedelta(hours=1))
                return {"url": url}
        
        return {"url": None}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.put("/prof/{username}")
async def update_profile_picture(username: str, file: UploadFile = File(...)):
    try:
        bucket = storage.bucket()
        blobs = bucket.list_blobs(prefix=f"profile/{username}/")
        
        for blob in blobs:
            if blob.name.lower().endswith(('.png', '.jpg', '.jpeg', '.gif')):
                blob.delete()
                
        with tempfile.NamedTemporaryFile(delete=False) as temp_file:
            content = await file.read()
            temp_file.write(content)
            temp_file_path = temp_file.name

        path = f"profile/{username}/{file.filename}"
        
        bucket = storage.bucket()
        blob = bucket.blob(path)
        blob.upload_from_filename(temp_file_path)

        # Generate a signed URL that's valid for 1 hour
        url = blob.generate_signed_url(expiration=timedelta(hours=1))

        return {"file": file.filename, "url": url}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if 'temp_file_path' in locals():
            os.unlink(temp_file_path)

@router.delete("/prof/{username}")
async def delete_profile_picture(username: str):
    try:
        bucket = storage.bucket()
        blobs = bucket.list_blobs(prefix=f"profile/{username}/")
        
        deleted = False
        for blob in blobs:
            if blob.name.lower().endswith(('.png', '.jpg', '.jpeg', '.gif')):
                blob.delete()
                deleted = True

        if deleted:
            return {"message": "Profile picture deleted successfully"}
        else:
            return {"message": "No profile picture found"}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
