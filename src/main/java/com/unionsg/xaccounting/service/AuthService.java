package com.unionsg.xaccounting.service;

import com.unionsg.xaccounting.dto.LoginRequest;
import com.unionsg.xaccounting.dto.RegisterRequest;
import com.unionsg.xaccounting.entity.UserEntity;
import com.unionsg.xaccounting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {
   private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder;

   public void register(RegisterRequest request){
       if (userRepository.existsByEmail(request.email())){
           throw new RuntimeException("Email  already registered");
       }

       UserEntity user = UserEntity.builder()
               .email(request.email())
               .password(passwordEncoder.encode(request.password()))
               .build();
       userRepository.save(user);
   }

   public String login(LoginRequest request) {
       UserEntity user = userRepository.findByEmail(request.email())
               .orElseThrow(()-> new RuntimeException("Invalid credentials"));

       if (!passwordEncoder.matches(request.password(), user.getPassword())){
           throw new RuntimeException("Invalid credentials");
       }
       return "Login successful";
   }
}
