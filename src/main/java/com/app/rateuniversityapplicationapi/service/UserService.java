package com.app.rateuniversityapplicationapi.service;

import com.app.rateuniversityapplicationapi.config.JwtService;
import com.app.rateuniversityapplicationapi.dto.AuthenticationRequest;
import com.app.rateuniversityapplicationapi.dto.AuthenticationResponse;
import com.app.rateuniversityapplicationapi.dto.RegisterRequest;
import com.app.rateuniversityapplicationapi.entity.Course;
import com.app.rateuniversityapplicationapi.dto.UserResponse;
import com.app.rateuniversityapplicationapi.entity.Role;
import com.app.rateuniversityapplicationapi.entity.User;
import com.app.rateuniversityapplicationapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    @Override
    public ResponseEntity<AuthenticationResponse> register(RegisterRequest request) {

        User fromDb = userRepository.findByEmail(request.getEmail());


        if (fromDb == null) {
            var user = User.builder()
                    .firstname(request.getFirstname())
                    .lastname(request.getLastname())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .birthDate(request.getBirthDate())
                    .phoneNumber(request.getPhoneNumber())
                    .role(Role.USER)
                    .build();

            userRepository.save(user);
            String jwtToken = jwtService.generateToken(user);
            return new ResponseEntity<>(AuthenticationResponse.builder()
                    .token(jwtToken)
                    .message("Registered Succesfully")
                    .validUntil(
                            jwtService.extractAllClaims(jwtToken)
                                    .getExpiration()
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate())
                    .build(), HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public int getNumberOfStudents() {
        return userRepository.getNumberOfStudents();
    }

    @Override
    public User findUserById(UUID uuid) {
        return userRepository.findUserById(uuid);
    }


    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail());

        String jwtToken = jwtService.generateToken(user);

        return new ResponseEntity<>(AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Logged in Succesfully")
                .validUntil(
                        jwtService.extractAllClaims(jwtToken)
                                .getExpiration()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate())
                .build(), HttpStatus.ACCEPTED).getBody();
    }


    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null; // No user authenticated
        }

        User userFromDb = userRepository.findByEmail(authentication.getName());
        return new UserResponse() {
            @Override
            public String getFirstName() {
                return userFromDb.getFirstname();
            }

            @Override
            public String getLastName() {
                return userFromDb.getLastname();
            }

            @Override
            public String getEmail() {
                return userFromDb.getEmail();
            }

            @Override
            public LocalDate getBirdhDate() {
                return userFromDb.getBirthDate();
            }

            @Override
            public String getPhoneNumber() {
                return userFromDb.getPhoneNumber();
            }

            @Override
            public String getProfilePhotoUrl() {
                return userFromDb.getProfilePhotoURL();
            }
        };
    }
}
