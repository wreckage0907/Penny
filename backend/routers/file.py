from fastapi import HTTPException, APIRouter
from database.firebase_init import firebase_storage

router = APIRouter()

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