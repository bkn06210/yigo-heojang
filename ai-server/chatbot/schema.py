"""Spring 과 주고받는 요청·응답 형식.

파이썬은 snake_case, 팀의 JSON 은 camelCase 라 alias 로 변환한다.
직접 camelCase 로 필드명을 적으면 파이썬 쪽이 어색해지고, 변환을 손으로 하면 빠뜨린다.

`{ success, data, message }` 봉투는 여기서 씌우지 않는다. 이 응답을 받는 쪽이
프론트가 아니라 Spring 이고, 봉투는 Spring 의 공통 클래스가 씌우기 때문이다.
봉투를 양쪽에서 씌우면 data 안에 data 가 들어간다.
"""

from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class ChatRequest(CamelModel):
    # Spring 이 JWT 를 검증한 뒤 꺼내 넘겨준다. 이 서버는 토큰을 다시 열지 않는다.
    member_id: int
    question: str = Field(min_length=1, max_length=500)


class ChatResponse(CamelModel):
    answer: str
    intent: str
    # 답변의 숫자가 어디서 나왔는지. 화면에 근거로 표시할 수 있게 함께 내려준다.
    sources: List[str] = Field(default_factory=list)
    # 분류만 되고 대상을 못 찾았을 때 되물을 문장. 없으면 null.
    follow_up_question: Optional[str] = None


class HealthResponse(CamelModel):
    status: str
    llm_provider: str
