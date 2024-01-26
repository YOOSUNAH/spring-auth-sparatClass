package com.sparta.springauth.service;

import com.sparta.springauth.dto.LoginRequestDto;
import com.sparta.springauth.dto.SignupRequestDto;
import com.sparta.springauth.entity.User;
import com.sparta.springauth.entity.UserRoleEnum;
import com.sparta.springauth.jwt.JwtUtil;
import com.sparta.springauth.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    // ADMIN_TOKEN
    private final String ADMIN_TOKEN = "AAABnvxRVklrnYxKZ0aHgTBcXukeZygoC";

    public void signup(SignupRequestDto requestDto) {
        String username = requestDto.getUsername();
        String password = passwordEncoder.encode(requestDto.getPassword());  // requestDto에서 가져온 Password 평문을 encode 암호화해서 password에 저장

        // 회원 중복 확인
        Optional<User> checkUsername = userRepository.findByUsername(username); // findByUsername메서드를 이용해서 username을 가져온다.
        // null check 하려고 Optional 로 받음.
        if (checkUsername.isPresent()) {  // Optional 내부에 isPresent 메서드 가 존재함 이를 이용.  값이 존재하는지 안하는지 확인해주는 메서드. 값이 있으면 true가 반환됨.
            // true 면, 값이 있다는 것이니, 중복된 사용자가 있다는 걸로 보고 throw 던짐.
            throw new IllegalArgumentException("중복된 사용자가 존재합니다.");
        }

        // email 중복확인
        String email = requestDto.getEmail();
        Optional<User> checkEmail = userRepository.findByEmail(email);
        if (checkEmail.isPresent()) {
            throw new IllegalArgumentException("중복된 Email 입니다.");
        }

        // 사용자 ROLE 확인
        UserRoleEnum role = UserRoleEnum.USER;
        if (requestDto.isAdmin()) {  //isAdmin 이 true 면 관리자 권한으로 회원가입하겠다. flase면 일반 회원으로
            // boolean 타입으로 오는건 is~ 이런식으로 이름 짓는다.)
            if (!ADMIN_TOKEN.equals(requestDto.getAdminToken())) {  // entity 클래스 객체를 만들어야 한다.
                // 데이터 베이스에 한줄 한 row는 해당하는 entity클래스의 하나의 객체다.
                // 가져온 토큰이 아무것도 없으면 일치하지 않으니 false가 된다. -> throw
               // equals가 ! 아닐때
                throw new IllegalArgumentException("관리자 암호가 틀려 등록이 불가능합니다.");
            }
            // equals 일 때
            role = UserRoleEnum.ADMIN;
        }

        // 사용자 등록
        User user = new User(username, password, email, role); // 여기 들어오는 role은 바로 위에서 ADMIN 권한 enum 값이 들어가있는 role이다.
        userRepository.save(user);  // 저장 됨.
    }

    public void login(LoginRequestDto requestDto, HttpServletResponse res) {
        String username = requestDto.getUsername();
        String password = requestDto.getPassword();

        // 사용자 확인
        User user =  userRepository.findByUsername(username).orElseThrow(
        () -> new IllegalArgumentException("등록된 사용자가 없습니다.")
        );

        // 비밀번호 확인
        if(!passwordEncoder.matches(password, user.getPassword())){  // mathces 기능 (평문, 암호화된 데이터-> entity객체로 가져오기)
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 생성 및 쿠키에 저장 후 response 객체에 추가 (addJwtToCookie 통해 response객체에 추가됨)
        String token = jwtUtil.createToken(user.getUsername(), user.getRole());
        jwtUtil.addJwtToCookie(token, res);

    }
}