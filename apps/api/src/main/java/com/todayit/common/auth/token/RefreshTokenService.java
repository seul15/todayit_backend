package com.todayit.common.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Refresh Token과 로그인 세션의 생성, 회전, 조회 및 삭제를 관리합니다.
 *
 * <p>로그인 세션 ID와 Refresh Token을 분리하여 관리합니다. Refresh Token 갱신 시 기존 토큰은 무효화하지만 세션 ID는 유지하므로 사용자의 로그인
 * 세션이 불필요하게 종료되지 않습니다.
 *
 * <p>Refresh Token 원문은 Redis에 저장하지 않고 SHA-256 해시값을 사용합니다. 현재 정책에 따라 로그인 세션과 Refresh Token에는 별도의
 * TTL을 설정하지 않습니다.
 */
@Service
public class RefreshTokenService {

  // Refresh Token 생성에 사용하는 난수의 바이트 길이
  private static final int TOKEN_BYTE_LENGTH = 32;

  // 로그인 세션 정보: sessionId -> memberId
  private static final String SESSION_KEY_PREFIX = "auth:refresh:";

  // Refresh Token 조회 정보: tokenHash -> sessionId
  private static final String TOKEN_KEY_PREFIX = "auth:refresh:token:";

  // 세션별 현재 유효한 Refresh Token: sessionId -> tokenHash
  private static final String CURRENT_TOKEN_KEY_PREFIX = "auth:refresh:current:";

  // 회원별 로그인 세션 목록: memberId -> Set<sessionId>
  private static final String MEMBER_KEY_PREFIX = "auth:refresh:member:";

  /**
   * 로그인 세션과 최초 Refresh Token 정보를 원자적으로 저장하는 Lua 스크립트입니다.
   *
   * <p>KEYS[1]: 로그인 세션 Key KEYS[2]: Refresh Token 조회 Key KEYS[3]: 현재 Refresh Token Key KEYS[4]: 회원별
   * 세션 목록 Key
   *
   * <p>ARGV[1]: 회원 ID ARGV[2]: 세션 ID ARGV[3]: Refresh Token 해시
   *
   * <p>반환값: 1(생성 성공), 0(Key 중복)
   */
  private static final RedisScript<Long> CREATE_SCRIPT =
      RedisScript.of(
          """
                  -- 동일한 세션 또는 토큰 정보가 이미 존재하면 생성하지 않음
                  if redis.call('EXISTS', KEYS[1]) == 1
                      or redis.call('EXISTS', KEYS[2]) == 1
                      or redis.call('EXISTS', KEYS[3]) == 1 then
                    return 0
                  end

                  -- 세션 ID에 회원 ID 연결
                  redis.call('SET', KEYS[1], ARGV[1])

                  -- Refresh Token 해시에 세션 ID 연결
                  redis.call('SET', KEYS[2], ARGV[2])

                  -- 세션에서 현재 유효한 Refresh Token 해시 저장
                  redis.call('SET', KEYS[3], ARGV[3])

                  -- 회원의 로그인 세션 목록에 신규 세션 추가
                  redis.call('SADD', KEYS[4], ARGV[2])

                  return 1
                  """,
          Long.class);

  /**
   * 기존 Refresh Token을 검증하고 새 토큰으로 교체하는 Lua 스크립트입니다.
   *
   * <p>토큰 검증과 교체를 하나의 Redis 작업으로 처리하여 동일한 Refresh Token으로 동시에 여러 갱신 요청이 발생하는 경우 하나의 요청만 성공하도록 합니다.
   *
   * <p>KEYS[1]: 로그인 세션 Key KEYS[2]: 현재 Refresh Token Key KEYS[3]: 기존 Refresh Token 조회 Key KEYS[4]:
   * 새로운 Refresh Token 조회 Key
   *
   * <p>ARGV[1]: 회원 ID ARGV[2]: 기존 Refresh Token 해시 ARGV[3]: 세션 ID ARGV[4]: 새로운 Refresh Token 해시
   *
   * <p>반환값: 1(회전 성공), 0(검증 실패 또는 동시 갱신 충돌)
   */
  private static final RedisScript<Long> ROTATE_SCRIPT =
      RedisScript.of(
          """
                  -- 요청 회원과 현재 로그인 세션의 소유자가 일치하는지 확인
                  if redis.call('GET', KEYS[1]) ~= ARGV[1]
                      -- 요청한 토큰이 현재 유효한 Refresh Token인지 확인
                      or redis.call('GET', KEYS[2]) ~= ARGV[2]
                      -- 기존 토큰이 현재 세션에 연결되어 있는지 확인
                      or redis.call('GET', KEYS[3]) ~= ARGV[3]
                      -- 새로운 토큰 해시가 이미 등록되어 있으면 거부
                      or redis.call('EXISTS', KEYS[4]) == 1 then
                    return 0
                  end

                  -- 기존 Refresh Token을 즉시 무효화
                  redis.call('DEL', KEYS[3])

                  -- 새 Refresh Token을 기존 세션 ID에 연결
                  redis.call('SET', KEYS[4], ARGV[3])

                  -- 현재 유효한 토큰 해시 갱신
                  redis.call('SET', KEYS[2], ARGV[4])

                  return 1
                  """,
          Long.class);

  /**
   * 현재 Refresh Token에 연결된 로그인 세션을 종료하는 Lua 스크립트입니다.
   *
   * <p>회원과 토큰의 소유 관계를 검증한 뒤 세션 정보, 토큰 정보, 회원별 세션 목록을 함께 정리합니다.
   *
   * <p>KEYS[1]: 로그인 세션 Key KEYS[2]: 현재 Refresh Token Key KEYS[3]: Refresh Token 조회 Key KEYS[4]: 회원별
   * 세션 목록 Key
   *
   * <p>ARGV[1]: 회원 ID ARGV[2]: Refresh Token 해시 ARGV[3]: 세션 ID
   *
   * <p>반환값: 1(삭제 성공), 0(검증 실패)
   */
  private static final RedisScript<Long> DELETE_SCRIPT =
      RedisScript.of(
          """
                  -- 해당 회원의 현재 Refresh Token인지 확인
                  if redis.call('GET', KEYS[1]) ~= ARGV[1]
                      or redis.call('GET', KEYS[2]) ~= ARGV[2]
                      or redis.call('GET', KEYS[3]) ~= ARGV[3] then
                    return 0
                  end

                  -- 현재 기기의 세션 및 토큰 정보 삭제
                  redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])

                  -- 회원별 세션 목록에서 해당 세션 제거
                  redis.call('SREM', KEYS[4], ARGV[3])

                  return 1
                  """,
          Long.class);

  /**
   * 회원의 모든 로그인 세션을 삭제하는 Lua 스크립트입니다.
   *
   * <p>비밀번호 변경이나 회원 탈퇴 등 모든 기기의 로그인을 종료해야 하는 상황에서 사용합니다.
   *
   * <p>KEYS[1]: 회원별 세션 목록 Key
   *
   * <p>ARGV[1]: 로그인 세션 Key prefix ARGV[2]: 현재 Refresh Token Key prefix ARGV[3]: Refresh Token 조회
   * Key prefix
   *
   * <p>반환값: 삭제 대상 세션 수
   */
  private static final RedisScript<Long> DELETE_ALL_SCRIPT =
      RedisScript.of(
          """
                  -- 회원에게 연결된 모든 로그인 세션 ID 조회
                  local sessions = redis.call('SMEMBERS', KEYS[1])

                  for _, sessionId in ipairs(sessions) do
                    -- 각 세션에 연결된 현재 Refresh Token 해시 조회
                    local tokenHash = redis.call('GET', ARGV[2] .. sessionId)

                    if tokenHash then
                      -- Refresh Token 조회 정보 삭제
                      redis.call('DEL', ARGV[3] .. tokenHash)
                    end

                    -- 세션 정보와 현재 토큰 정보 삭제
                    redis.call('DEL', ARGV[1] .. sessionId, ARGV[2] .. sessionId)
                  end

                  -- 회원별 로그인 세션 목록 삭제
                  redis.call('DEL', KEYS[1])

                  return #sessions
                  """,
          Long.class);

  private final StringRedisTemplate redisTemplate;
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Refresh Token과 로그인 세션 관리에 필요한 Redis 접근 객체를 주입받습니다.
   *
   * @param redisTemplate 문자열 기반 Redis 접근 객체
   */
  public RefreshTokenService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 회원의 로그인 세션과 최초 Refresh Token을 생성합니다.
   *
   * <p>Refresh Token과 세션 ID를 각각 생성하고 Redis에 저장합니다. 세션 ID는 이후 Refresh Token이 회전하더라도 변경되지 않습니다.
   *
   * @param memberId 로그인한 회원 식별자
   * @return 생성된 Refresh Token과 로그인 세션 ID
   * @throws IllegalStateException 세션 정보 생성에 실패한 경우
   */
  public RefreshTokenResult create(String memberId) {

    // 클라이언트에 전달할 Refresh Token 생성
    String token = generateToken();

    // Redis에는 토큰 원문 대신 해시값을 저장
    String tokenHash = hash(token);

    // Refresh Token과 독립적인 로그인 세션 ID 생성
    String sessionId = UUID.randomUUID().toString();

    // 세션 정보와 토큰 정보를 원자적으로 저장
    Long created =
        redisTemplate.execute(
            CREATE_SCRIPT,
            List.of(
                SESSION_KEY_PREFIX + sessionId,
                TOKEN_KEY_PREFIX + tokenHash,
                CURRENT_TOKEN_KEY_PREFIX + sessionId,
                MEMBER_KEY_PREFIX + memberId),
            memberId,
            sessionId,
            tokenHash);

    if (!Long.valueOf(1L).equals(created)) {
      throw new IllegalStateException("로그인 세션 생성에 실패했습니다.");
    }

    // Refresh Token 원문은 서버에 저장하지 않고 클라이언트에 반환
    return new RefreshTokenResult(token, sessionId);
  }

  /**
   * Refresh Token에 연결된 회원 식별자를 조회합니다.
   *
   * <p>Refresh Token 해시로 세션 ID를 찾은 뒤 해당 세션에 연결된 회원 식별자를 조회합니다.
   *
   * @param token 조회할 Refresh Token
   * @return 회원 식별자, 토큰이 존재하지 않으면 empty
   */
  public Optional<String> findMemberId(String token) {

    // Refresh Token으로 로그인 세션 ID 조회
    return findSessionId(token)
        .map(sessionId -> redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId));
  }

  /**
   * 기존 Refresh Token을 무효화하고 새로운 Refresh Token을 발급합니다.
   *
   * <p>토큰 갱신 시 새로운 Refresh Token을 생성하지만 로그인 세션 ID는 유지하여 기존 로그인 상태를 보존합니다.
   *
   * <p>기존 토큰 검증과 신규 토큰 등록은 Lua 스크립트에서 원자적으로 처리합니다. 동일한 기존 토큰으로 중복 요청이 발생하면 하나의 회전만 성공할 수 있습니다.
   *
   * @param token 기존 Refresh Token
   * @param memberId Refresh Token을 소유한 회원 식별자
   * @return 새로운 Refresh Token과 기존 세션 ID, 회전에 실패하면 empty
   */
  public Optional<RefreshTokenResult> rotate(String token, String memberId) {

    // 기존 Refresh Token을 해시로 변환
    String oldTokenHash = hash(token);

    // 기존 토큰이 연결된 로그인 세션 조회
    String sessionId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + oldTokenHash);

    if (sessionId == null) {
      return Optional.empty();
    }

    // 새롭게 사용할 Refresh Token 및 해시 생성
    String newToken = generateToken();
    String newTokenHash = hash(newToken);

    // 기존 토큰 유효성 검사와 새 토큰 등록을 원자적으로 처리
    Long rotated =
        redisTemplate.execute(
            ROTATE_SCRIPT,
            List.of(
                SESSION_KEY_PREFIX + sessionId,
                CURRENT_TOKEN_KEY_PREFIX + sessionId,
                TOKEN_KEY_PREFIX + oldTokenHash,
                TOKEN_KEY_PREFIX + newTokenHash),
            memberId,
            oldTokenHash,
            sessionId,
            newTokenHash);

    // 기존 토큰이 이미 사용됐거나 세션이 종료된 경우 갱신 거부
    if (!Long.valueOf(1L).equals(rotated)) {
      return Optional.empty();
    }

    // 기존 세션 ID를 유지한 채 새로운 Refresh Token 반환
    return Optional.of(new RefreshTokenResult(newToken, sessionId));
  }

  /**
   * Refresh Token에 연결된 현재 기기의 로그인 세션을 종료합니다.
   *
   * <p>전달받은 토큰이 현재 회원에게 발급된 유효한 토큰인지 확인한 뒤 해당 세션과 관련 Redis 데이터를 삭제합니다.
   *
   * <p>다른 기기에서 사용 중인 로그인 세션은 유지합니다.
   *
   * @param token 삭제할 Refresh Token
   * @param memberId 로그아웃할 회원 식별자
   */
  public void delete(String token, String memberId) {

    String tokenHash = hash(token);

    // Refresh Token에 연결된 세션 ID 조회
    String sessionId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + tokenHash);

    // 존재하지 않거나 이미 무효화된 토큰이면 종료
    if (sessionId == null) {
      return;
    }

    // 회원과 토큰의 소유 관계 확인 후 현재 세션 삭제
    redisTemplate.execute(
        DELETE_SCRIPT,
        List.of(
            SESSION_KEY_PREFIX + sessionId,
            CURRENT_TOKEN_KEY_PREFIX + sessionId,
            TOKEN_KEY_PREFIX + tokenHash,
            MEMBER_KEY_PREFIX + memberId),
        memberId,
        tokenHash,
        sessionId);
  }

  /**
   * 회원에게 발급된 모든 로그인 세션을 종료합니다.
   *
   * <p>회원별 세션 목록을 조회한 뒤 해당 회원의 모든 Refresh Token과 로그인 세션 정보를 삭제합니다.
   *
   * <p>비밀번호 변경이나 회원 탈퇴 등 모든 기기의 인증 상태를 무효화해야 하는 경우 사용할 수 있습니다.
   *
   * @param memberId 모든 로그인 세션을 종료할 회원 식별자
   */
  public void deleteAll(String memberId) {

    // 회원의 모든 세션 및 Refresh Token 데이터를 원자적으로 삭제
    redisTemplate.execute(
        DELETE_ALL_SCRIPT,
        List.of(MEMBER_KEY_PREFIX + memberId),
        SESSION_KEY_PREFIX,
        CURRENT_TOKEN_KEY_PREFIX,
        TOKEN_KEY_PREFIX);
  }

  /**
   * Access Token에 포함된 로그인 세션이 현재 유효한지 확인합니다.
   *
   * <p>JWT의 세션 ID로 Redis에서 세션 소유자를 조회하고 Access Token에 포함된 회원 식별자와 일치하는지 확인합니다.
   *
   * <p>로그아웃이나 전체 세션 삭제로 Redis 정보가 제거된 경우 Access Token의 만료 시간이 남아 있더라도 인증을 허용하지 않습니다.
   *
   * @param sessionId Access Token에 포함된 로그인 세션 ID
   * @param memberId Access Token에 포함된 회원 식별자
   * @return 세션이 존재하고 회원 식별자가 일치하면 true
   */
  public boolean isSessionActive(String sessionId, String memberId) {

    // Redis에서 로그인 세션의 소유 회원 확인
    String storedMemberId = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId);

    return memberId.equals(storedMemberId);
  }

  /**
   * Refresh Token으로 로그인 세션 ID를 조회합니다.
   *
   * @param token 조회할 Refresh Token
   * @return 연결된 세션 ID, 토큰이 존재하지 않으면 empty
   */
  private Optional<String> findSessionId(String token) {

    String sessionId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + hash(token));

    return Optional.ofNullable(sessionId);
  }

  /**
   * 암호학적으로 안전한 난수로 Refresh Token을 생성합니다.
   *
   * <p>32바이트 난수를 URL-safe Base64 문자열로 변환하며 문자열 끝의 padding 문자는 제거합니다.
   *
   * @return 새롭게 생성한 Refresh Token 문자열
   */
  private String generateToken() {

    byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];

    secureRandom.nextBytes(tokenBytes);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  /**
   * Refresh Token의 SHA-256 해시값을 생성합니다.
   *
   * <p>토큰 원문을 Redis에 저장하지 않기 위해 사용합니다. 생성된 해시는 URL-safe Base64 문자열로 변환합니다.
   *
   * @param token 해시를 생성할 Refresh Token
   * @return SHA-256 해시의 URL-safe Base64 문자열
   * @throws IllegalStateException SHA-256 알고리즘을 사용할 수 없는 경우
   */
  private String hash(String token) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);

    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Refresh Token 해시 생성에 실패했습니다.", exception);
    }
  }
}
