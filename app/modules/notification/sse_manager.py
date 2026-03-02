"""SSE 연결 관리자.

in-memory Queue 기반으로 사용자별 SSE 스트림을 관리한다.
서버의 모든 라우트가 동기(def)이므로 sync Queue를 사용한다.
StreamingResponse는 sync generator를 iterate_in_threadpool로 래핑하여
이벤트 루프를 차단하지 않는다.
"""

import uuid
from queue import Queue
from threading import Lock


class SSEManager:
    """사용자별 SSE 연결을 관리하는 싱글턴."""

    def __init__(self) -> None:
        self._connections: dict[uuid.UUID, list[Queue]] = {}
        self._lock = Lock()

    def connect(self, user_id: uuid.UUID) -> Queue:
        """SSE 연결 시 Queue 생성·등록."""
        q: Queue = Queue()
        with self._lock:
            self._connections.setdefault(user_id, []).append(q)
        return q

    def disconnect(self, user_id: uuid.UUID, q: Queue) -> None:
        """SSE 연결 해제."""
        with self._lock:
            queues = self._connections.get(user_id)
            if queues is None:
                return
            try:
                queues.remove(q)
            except ValueError:
                pass
            if not queues:
                del self._connections[user_id]

    def push(self, user_id: uuid.UUID, data: str) -> None:
        """해당 사용자의 모든 연결에 이벤트 전송."""
        with self._lock:
            queues = self._connections.get(user_id, [])
            for q in queues:
                q.put(data)


sse_manager = SSEManager()
