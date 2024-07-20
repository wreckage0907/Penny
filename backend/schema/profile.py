from pydantic import BaseModel

class ProfilePictureResponse(BaseModel):
    file: str
    url: str

class ProfilePictureURL(BaseModel):
    url: str

class ProfilePictureDeleteResponse(BaseModel):
    message: str
