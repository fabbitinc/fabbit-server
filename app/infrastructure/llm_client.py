"""LLM 호출 래퍼.

LangChain을 사용하여 LLM 호출을 추상화합니다.
모델 교체 시 이 파일만 수정하면 됩니다.
"""

import time
from dataclasses import dataclass

from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage
from loguru import logger

from app.core.config import settings

DEFAULT_MODEL = "gpt-5-mini"


@dataclass
class LLMResponse:
    """LLM 응답 + 토큰 사용량."""

    content: str
    model: str
    input_tokens: int
    output_tokens: int


def _create_llm(
    model: str = DEFAULT_MODEL,
    temperature: float = 0,
    max_tokens: int | None = None,
    response_format: dict | None = None,
) -> ChatOpenAI:
    """ChatOpenAI 인스턴스 생성"""
    kwargs = {
        "model": model,
        "temperature": temperature,
        "api_key": settings.openai_api_key,
    }
    if max_tokens is not None:
        kwargs["max_tokens"] = max_tokens
    if response_format:
        kwargs["model_kwargs"] = {"response_format": response_format}
    return ChatOpenAI(**kwargs)


def _strip_code_block(content: str) -> str:
    """코드 블록 마커 제거."""
    if content.startswith("```"):
        lines = content.split("\n")
        content = "\n".join(lines[1:-1] if lines[-1].startswith("```") else lines[1:])
    return content.strip()


def chat_completion(
    system_prompt: str,
    user_message: str,
    model: str = DEFAULT_MODEL,
    temperature: float = 0,
    response_format: dict | None = None,
) -> str:
    """LLM 채팅 완성 호출 후 텍스트 응답 반환"""
    resp = chat_completion_with_usage(
        system_prompt=system_prompt,
        user_message=user_message,
        model=model,
        temperature=temperature,
        response_format=response_format,
    )
    return resp.content


def chat_completion_with_usage(
    system_prompt: str,
    user_message: str,
    model: str = DEFAULT_MODEL,
    temperature: float = 0,
    response_format: dict | None = None,
) -> LLMResponse:
    """LLM 채팅 완성 호출 후 텍스트 + 토큰 사용량 반환"""
    llm = _create_llm(model=model, temperature=temperature, response_format=response_format)

    messages = [
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_message),
    ]

    prompt_preview = user_message[:80].replace("\n", " ")
    logger.info("[LLM] 호출 시작: model={model} prompt={prompt}...", model=model, prompt=prompt_preview)
    t0 = time.perf_counter()

    response = llm.invoke(messages)

    elapsed = time.perf_counter() - t0
    content = _strip_code_block(response.content.strip())

    # LangChain AIMessage.usage_metadata → 토큰 사용량
    usage = getattr(response, "usage_metadata", None) or {}
    input_tokens = usage.get("input_tokens", 0) if isinstance(usage, dict) else getattr(usage, "input_tokens", 0)
    output_tokens = usage.get("output_tokens", 0) if isinstance(usage, dict) else getattr(usage, "output_tokens", 0)

    logger.info(
        "[LLM] 호출 완료: {elapsed:.1f}s | in={in_tok} out={out_tok} tokens | model={model}",
        elapsed=elapsed,
        in_tok=input_tokens,
        out_tok=output_tokens,
        model=model,
    )

    return LLMResponse(
        content=content,
        model=model,
        input_tokens=input_tokens,
        output_tokens=output_tokens,
    )
