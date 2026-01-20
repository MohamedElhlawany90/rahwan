
package com.blueWhale.Rahwan.user;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
//import com.blueWhale.Rahwan.security.jwt.JwtTokenProvider;
import com.blueWhale.Rahwan.util.ImageUtility;
import com.blueWhale.Rahwan.wallet.Wallet;
import com.blueWhale.Rahwan.wallet.WalletDto;
import com.blueWhale.Rahwan.wallet.WalletMapper;
import com.blueWhale.Rahwan.wallet.WalletService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

@Service
@Transactional
public class UserService {

    private static final String UPLOADED_FOLDER = "/home/ubuntu/rahwan/";
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final WalletService walletService;
    private final WalletMapper walletMapper;
    private final PasswordEncoder passwordEncoder ;
//    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       WalletService walletService, WalletMapper walletMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.walletService = walletService;
        this.walletMapper = walletMapper;
        this.passwordEncoder = passwordEncoder;
//        this.jwtTokenProvider = jwtTokenProvider;
    }

    public UserDto createUser(UserForm form) {

        validatePhone(form.getPhone());

       userRepository.findByPhone(form.getPhone())
               .ifPresent(user -> {
                   throw new ResourceNotFoundException("Phone already exists: " + form.getPhone());
               });

       User user = userMapper.toEntity(form);
       user.setPassword(passwordEncoder.encode(user.getPassword()));
       User saved = userRepository.save(user);

       walletService.createWalletForUser(saved);

        return userMapper.toDto(saved);
    }


    public UserDto signIn(String phone, String password) {
        validatePhone(phone);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid phone or password");
        }

        if (!user.isVerifiedPhone()) {
            throw new RuntimeException("Phone not verified");
        }

        if (!user.isActive()) {
            user.setActive(true);
            userRepository.save(user);
        }
//        String token = jwtTokenProvider
//                .generateToken(user.getId(), user.getPhone(), user.getType());
//
//        UserDto userDto = userMapper.toDto(user);
//        userDto.setToken(token);
//        return userDto;
        return userMapper.toDto(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserDto getUserById(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    public UserDto updateUser(UUID id, UserForm form){
        validatePhone(form.getPhone());

        User user = userRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.findByPhone(form.getPhone())
                .filter(existingUser ->!existingUser.getId().equals(id))
                .ifPresent(u ->{
                    throw new RuntimeException("Phone already exists");
                });

        user.setName(form.getName());
        user.setPhone(form.getPhone());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setType(form.getType());

        User updated = userRepository.save(user);
        return userMapper.toDto(updated);

    }

    public UserDto updateProfile(UUID userId, UpdateProfileForm form) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        validatePhone(form.getPhone());

        userRepository.findByPhone(form.getPhone())
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u-> {
                    throw new RuntimeException("Phone already exists");
                });

        user.setName(form.getName());
        user.setPhone(form.getPhone());

        // هنا عرفنا uploadDir
        Path uploadDir = Paths.get(UPLOADED_FOLDER);

        if (form.getProfileImage() != null && !form.getProfileImage().isEmpty()) {

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            byte[] bytes = ImageUtility.compressImage(form.getProfileImage().getBytes());

            String fileName = new Date().getTime()
                    + "A-A"
                    + form.getProfileImage().getOriginalFilename();

            Path path = uploadDir.resolve(fileName);

            Files.write(path, bytes);

            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);

            user.setProfileImage(fileName); // ✅ المهم ده
        }


        user.setActive(true);

            User saved = userRepository.save(user);
            return userMapper.toDto(saved);
    }

   public void deleteUser(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with id:" + id));

        user.setActive(false);
        userRepository.save(user);
   }

    public UserDto reactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(true);
        User updated = userRepository.save(user);

        return userMapper.toDto(updated);
    }

    public WalletDto getUserWallet(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return walletMapper.toDto(wallet);
    }

    private void validatePhone(String phone){
        if(phone == null || !phone.startsWith("20") || phone.length() != (12)){
            throw new RuntimeException("Phone must start with 20 and be 12 digits");
        }
    }

//    private String saveProfileImage(MultipartFile file){
//        try {
//            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
//            Path path = Paths.get("uploads/" + fileName);
//            Files.createDirectories(path.getParent());
//            Files.write(path, file.getBytes());
//            return fileName;
//        } catch (Exception e){
//            throw new RuntimeException("Failed to save image");
//        }
//    }
    public WalletDto updateUserBalance(UUID userId, double newBalance) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()){
            throw new RuntimeException("Cannot update balance for inactive user");
        }
        Wallet updatedWallet = walletService.updateUserBalance(userId, newBalance);
        return walletMapper.toDto(updatedWallet);
    }

}