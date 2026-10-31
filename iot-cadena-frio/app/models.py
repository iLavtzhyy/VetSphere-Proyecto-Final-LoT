from datetime import datetime, timedelta, timezone

from pydantic import BaseModel, Field, field_validator


class SensorReading(BaseModel):
    device_id: str = Field(min_length=3, max_length=80)
    temperature: float = Field(ge=-80, le=100)
    humidity: float | None = Field(default=None, ge=0, le=100)
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

    @field_validator("timestamp")
    @classmethod
    def timestamp_not_in_future(cls, value: datetime) -> datetime:
        compare = value if value.tzinfo else value.replace(tzinfo=timezone.utc)
        if compare > datetime.now(timezone.utc).replace(microsecond=0) + timedelta(minutes=5):
            raise ValueError("La marca de tiempo no puede estar más de cinco minutos en el futuro.")
        return value

