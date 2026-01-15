//package com.blueWhale.Rahwan.security;
//
//import com.blueWhale.Rahwan.user.User;
//import com.blueWhale.Rahwan.user.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
//        User user = userRepository.findByPhone(phone)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + phone));
//
//        return UserPrincipal.create(user);
//    }
//
//    public UserDetails loadUserById(UUID userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
//
//        return UserPrincipal.create(user);
//    }
//}