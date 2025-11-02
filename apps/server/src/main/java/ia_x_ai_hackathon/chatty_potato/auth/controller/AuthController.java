package ia_x_ai_hackathon.chatty_potato.auth.controller;

import ia_x_ai_hackathon.chatty_potato.common.resolver.UserId;
import ia_x_ai_hackathon.chatty_potato.common.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.OK)
    public void guest(HttpServletResponse response) {
        String guestId = "guest-" + UUID.randomUUID();
        String accessToken = jwtUtil.createAccessToken(guestId);

		ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
				.httpOnly(true)
				.secure(true)
				.sameSite("None")     // <- cross-site 쿠키 허용
				.path("/")
				.maxAge(7 * 24 * 60 * 60) // <- 7일
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

	@DeleteMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletResponse response) {
		ResponseCookie expired = ResponseCookie.from("accessToken", "")
				.httpOnly(true)
				.secure(true)
				.sameSite("None")     // <- 발급 때와 동일
				.path("/")
				.maxAge(0)            // <- 즉시 만료
				.build();

		response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());

		// refreshToken도 있다면 같은 방식으로 하나 더 내려주기
	}

	@GetMapping("/ping")
	@ResponseStatus(HttpStatus.OK)
	public String ping(@UserId String userId) {
		return userId;
	}

}
