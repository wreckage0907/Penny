from fastapi import HTTPException, APIRouter
from database.firebase_init import firebase_storage

router = APIRouter()

@router.get("/get-file/{subfolder}/{filename}")
async def get_file(subfolder: str, filename: str):
    try:
        file_path = f"content/{subfolder}/{filename}.md"
        blob = firebase_storage.blob(file_path)
        content = blob.download_as_text()
        if content:
            return {"content": content}
        else:
            raise HTTPException(status_code=404, detail="File not found")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/subfolder")
async def sub():
    try:
        blobs = firebase_storage.list_blobs(prefix='content/')
        folder_names = set()
        for blob in blobs:
            parts = blob.name.split('/')
            if len(parts) > 1:
                folder_names.add(parts[1])

        return {"title": folder_names}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))