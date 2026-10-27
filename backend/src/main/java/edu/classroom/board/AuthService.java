package edu.classroom.board;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HexFormat;

@Service
public class AuthService {
  private final SecretKey key;
  private final String issuer;
  private final String audience;
  private final int ttl;
  private final Users users;

  public AuthService(
          @Value("${app.jwt.secret}") String secret,
          @Value("${app.jwt.issuer}") String issuer,
          @Value("${app.jwt.audience}") String audience,
          @Value("${app.jwt.ttl}") int ttl,
          Users users) {
    if (secret == null || !secret.matches("[0-9a-fA-F]{64}")
            || secret.equals("REPLACE_WITH_RANDOM_64_HEX_CHARACTERS")) {
      throw new IllegalStateException("JWT_SECRET은 64자리 hex 문자열이어야 합니다.");
    }
    this.key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secret));
    this.issuer = issuer;
    this.audience = audience;
    this.ttl = ttl;
    this.users = users;
  }

  public String issueToken(Users.PublicUser user) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + ttl * 1000L);
    return Jwts.builder()
            .subject(String.valueOf(user.id()))
            .issuer(issuer)
            .audience().add(audience).and()
            .issuedAt(now)
            .expiration(exp)
            .signWith(key, Jwts.SIG.HS256)
            .compact();
  }

  public long currentUserId(HttpServletRequest request) {
    // 1. 헤더에서 토큰 꺼내기
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다.");
    }
    String token = header.substring(7).trim();
    if (token.isEmpty()) {
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다.");
    }

    // 2. 라이브러리로 서명·iss·aud·만료 검증
    Claims claims;
    try {
      Jws<Claims> jws = Jwts.parser()
              .verifyWith(key)
              .requireIssuer(issuer)
              .requireAudience(audience)
              .build()
              .parseSignedClaims(token);
      if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
        throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
      }
      claims = jws.getPayload();
    } catch (ExpiredJwtException e) {
      throw new ApiError(401, "TOKEN_EXPIRED", "로그인 시간이 만료되었습니다. 다시 로그인하세요.");
    } catch (JwtException | IllegalArgumentException e) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }

    // 3. exp가 있는지, sub가 양의 정수인지
    String sub = claims.getSubject();
    if (claims.getExpiration() == null || sub == null || !sub.matches("[1-9][0-9]{0,17}")) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }

    // 4. 실제로 있는 유저인지
    long id = Long.parseLong(sub);
    if (users.find(id) == null) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }
    return id;
  }

  public void assertOwner(long userId, long authorId) {
    // TODO BE-03
  }
}