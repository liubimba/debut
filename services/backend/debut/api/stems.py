from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from starlette.requests import Request
from starlette.responses import FileResponse

from debut.api.dependencies import StemsStoreDep

router = APIRouter(prefix="/stems", tags=["Stems"])


class StemResponse(BaseModel):
    name: str
    url: str


@router.get("/")
def list_all_stems(
    stems_store: StemsStoreDep, request: Request
) -> dict[str, list[StemResponse]]:
    return {
        stem_id: [
            StemResponse(
                name=stem.name,
                url=str(
                    request.url_for("get_stem", stem_id=stem_id, stem_name=stem.name)
                ),
            )
            for stem in stems
        ]
        for stem_id, stems in stems_store.list_all_stems().items()
    }


@router.get("/{stem_id}")
def list_stems(
    stem_id: str, stems_store: StemsStoreDep, request: Request
) -> list[StemResponse]:
    try:
        stems = stems_store.list_stems(stem_id)
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
    return [
        StemResponse(
            name=stem.name,
            url=str(request.url_for("get_stem", stem_id=stem_id, stem_name=stem.name)),
        )
        for stem in stems
    ]


@router.get("/{stem_id}/{stem_name}")
def get_stem(stem_id: str, stem_name: str, stems_store: StemsStoreDep) -> FileResponse:
    try:
        stems = stems_store.list_stems(stem_id)
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
    try:
        stem = next(stem for stem in stems if stem.name == stem_name)
    except StopIteration:
        raise HTTPException(status_code=404, detail="Stem not found") from None
    return FileResponse(path=stem.path, filename=stem.name)
