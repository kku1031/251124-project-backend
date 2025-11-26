package store.kanggyoenggu.authservice.dto;

/**
 * 사용자 정보 응답 DTO
 */
public class UserInfoResponse {

    private boolean success;
    private String message;
    private UserData user;

    public UserInfoResponse() {
    }

    public UserInfoResponse(boolean success, String message, UserData user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    // Static factory methods
    public static UserInfoResponse success(UserData user) {
        return new UserInfoResponse(true, "사용자 정보 조회 성공", user);
    }

    public static UserInfoResponse error(String message) {
        return new UserInfoResponse(false, message, null);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserData getUser() {
        return user;
    }

    public void setUser(UserData user) {
        this.user = user;
    }

    /**
     * 사용자 데이터
     */
    public static class UserData {
        private String id;
        private Long kakaoId;
        private String nickname;

        public UserData() {
        }

        public UserData(String id, Long kakaoId, String nickname) {
            this.id = id;
            this.kakaoId = kakaoId;
            this.nickname = nickname;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Long getKakaoId() {
            return kakaoId;
        }

        public void setKakaoId(Long kakaoId) {
            this.kakaoId = kakaoId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}

