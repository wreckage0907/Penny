import os
from fastapi import HTTPException, APIRouter
from firebase_admin import credentials, initialize_app, storage

router = APIRouter()

current = os.path.dirname(__file__)
path = os.path.join(current, "../firebase.json") 
cred = credentials.Certificate(path)
firebase_app = initialize_app(cred, {
    'storageBucket': 'penny-89979.appspot.com'
})
firebase_storage = storage.bucket()

@router.get("/get-file/{folder}/{subfolder}/{filename}")
async def get_file(folder: str, subfolder: str, filename: str):
    try:
        file_path = f"{folder}/{subfolder}/{filename}"
        blob = firebase_storage.blob(file_path)
        content = blob.download_as_text()

        if content:
            return {"content": content}
        else:
            raise HTTPException(status_code=404, detail="File not found")
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
