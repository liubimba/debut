import logging
from argparse import ArgumentParser
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import uvicorn
from fastapi import APIRouter, FastAPI

from debut.api.jobs_router import router as jobs_router
from debut.api.transcriber import router as transcriber_router
from debut.core.builder import BackendBuilder
from debut.logging_config import configure_logging

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    logger.info("building backend context")
    ctx = await BackendBuilder().build()
    for attr, value in ctx.__dict__.items():
        setattr(app.state, attr, value)
    logger.info("backend ready")
    try:
        yield
    finally:
        pass


router = APIRouter(prefix="/api/v1")
router.include_router(transcriber_router)
router.include_router(jobs_router)

app = FastAPI(title="Debut Backend API", version="0.0.1", lifespan=lifespan)
app.include_router(router)


def run(port: int, host: str) -> None:
    uvicorn.run(app, host=host, port=port)


def main() -> None:
    parser = ArgumentParser(prog="debut-backend")
    parser.add_argument("--port", type=int, default=4999)
    parser.add_argument("--host", type=str, default="0.0.0.0")
    parser.add_argument("--log-level", type=str, default="INFO")
    args = parser.parse_args()

    level = logging.getLevelNamesMapping().get(args.log_level.upper(), logging.INFO)
    configure_logging(level=level)
    logger.info("starting debut-backend on %s:%s", args.host, args.port)
    run(args.port, args.host)
