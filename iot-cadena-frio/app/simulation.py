"""Generador de lecturas ficticias: no comparte credenciales ni rutas de sensores físicos."""

from __future__ import annotations

import asyncio
import logging
import random
from typing import Any, Callable

from .models import SensorReading

logger = logging.getLogger("vetsphere.cold-chain.simulation")


class SimulationEngine:
    def __init__(self, read_sensors: Callable, save_reading: Callable, broadcast: Callable):
        self.read_sensors = read_sensors
        self.save_reading = save_reading
        self.broadcast = broadcast
        self.running = False
        self.interval_seconds = 5
        self.mode = "NORMAL"
        self.task: asyncio.Task | None = None
        self.tick = 0

    def status(self) -> dict[str, Any]:
        return {"running": self.running, "intervalSeconds": self.interval_seconds,
                "mode": self.mode, "simulated": True}

    async def start(self, interval: int | None = None, mode: str | None = None) -> dict[str, Any]:
        if interval is not None:
            self.interval_seconds = interval
        if mode is not None:
            self.mode = mode
        if not self.running:
            self.running = True
            self.task = asyncio.create_task(self._run(), name="vetsphere-iot-simulator")
        state = self.status()
        await self.broadcast({"type": "simulation", "data": state})
        return state

    async def stop(self) -> dict[str, Any]:
        self.running = False
        if self.task and not self.task.done():
            self.task.cancel()
            try:
                await self.task
            except asyncio.CancelledError:
                pass
        await self.broadcast({"type": "simulation", "data": self.status()})
        return self.status()

    async def restart(self, interval: int, mode: str, reconnect_sensors: Callable) -> dict[str, Any]:
        await self.stop()
        await asyncio.to_thread(reconnect_sensors)
        self.tick = 0
        return await self.start(interval, mode)

    async def force_critical(self, device_id: str, max_temperature: float) -> dict[str, Any]:
        temperature = round(max_temperature + random.uniform(1.0, 2.5), 2)
        payload = SensorReading(device_id=device_id, temperature=temperature)
        result = await asyncio.to_thread(self.save_reading, payload, "SIMULATED", True)
        await self.broadcast({"type": "reading", "data": result})
        if result.get("alert"):
            await self.broadcast({"type": "alert", "data": result["alert"]})
        return result

    async def _run(self) -> None:
        while self.running:
            self.tick += 1
            try:
                sensors = await asyncio.to_thread(self.read_sensors)
                for _, device_id, status, _, low, high in sensors:
                    if status != "ACTIVE":
                        continue
                    low, high = float(low), float(high)
                    if self.mode == "CRITICAL":
                        temperature = random.choice([low - random.uniform(0.7, 3.0), high + random.uniform(0.7, 3.0)])
                    elif self.mode == "WARNING":
                        temperature = random.choice([low + random.uniform(0.05, 0.45), high - random.uniform(0.05, 0.45)])
                    else:
                        center = (low + high) / 2
                        temperature = max(low + 0.8, min(high - 0.8, center + random.uniform(-1.1, 1.1)))
                    reading = SensorReading(device_id=device_id, temperature=round(temperature, 2),
                                             humidity=round(random.uniform(42, 66), 1))
                    result = await asyncio.to_thread(self.save_reading, reading, "SIMULATED", True)
                    await self.broadcast({"type": "reading", "data": result})
                    if result.get("alert"):
                        await self.broadcast({"type": "alert", "data": result["alert"]})
            except asyncio.CancelledError:
                raise
            except Exception:
                logger.exception("No se pudo completar un ciclo del simulador")
            await asyncio.sleep(self.interval_seconds)

