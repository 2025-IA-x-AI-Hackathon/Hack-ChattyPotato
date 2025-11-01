package ia_x_ai_hackathon.chatty_potato.auth.controller;

import ia_x_ai_hackathon.chatty_potato.common.resolver.UserId;
import ia_x_ai_hackathon.chatty_potato.common.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

	@GetMapping("/ping")
	@ResponseStatus(HttpStatus.OK)
	public String ping(@UserId String userId) {
		return userId;
	}

}
