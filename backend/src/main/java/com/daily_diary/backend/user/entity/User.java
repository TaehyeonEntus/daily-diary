package com.daily_diary.backend.user.entity;

import com.daily_diary.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    public static User of(String username, String password, String nickname) {
        User user = new User();
        user.username = username;
        user.password = password;
        user.nickname = nickname;
        return user;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}
