"""LLM 호출 래퍼.

LangChain을 사용하여 LLM 호출을 추상화합니다.
모델 교체 시 이 파일만 수정하면 됩니다.
"""

from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage

from app.core.config import settings

DEFAULT_MODEL = "gpt-5-mini"


def _create_llm(
    model: str = DEFAULT_MODEL,
    temperature: float = 0,
    response_format: dict | None = None,
) -> ChatOpenAI:
    """ChatOpenAI 인스턴스 생성"""
    kwargs = {
        "model": model,
        "temperature": temperature,
        "api_key": settings.openai_api_key,
    }
    if response_format:
        kwargs["model_kwargs"] = {"response_format": response_format}
    return ChatOpenAI(**kwargs)


def chat_completion(
    system_prompt: str,
    user_message: str,
    model: str = DEFAULT_MODEL,
    temperature: float = 0,
    response_format: dict | None = None,
) -> str:
    """LLM 채팅 완성 호출 후 텍스트 응답 반환"""
    llm = _create_llm(model=model, temperature=temperature, response_format=response_format)

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_message),
    ]

    response = llm.invoke(messages)
    content = response.content.strip()

    # 코드 블록 마커 제거
    if content.startswith("```"):
        lines = content.split("\n")
        content = "\n".join(lines[1:-1] if lines[-1].startswith("```") else lines[1:])

    return content.strip()
