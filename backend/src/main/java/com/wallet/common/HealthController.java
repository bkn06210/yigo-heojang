package com.wallet.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 헬스체크: 서버와 스프링 컨텍스트가 정상 기동했는지 확인한다.
 * DB 연결과 무관하게 200을 반환한다(뼈대 검증이 MySQL 설치에 묶이지 않도록).
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }
}
