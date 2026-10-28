package com.agristep.repository;

import com.agristep.domain.User;

import java.util.Optional;

public interface UserRepository extends Repository<User, Long> {
    /** Looks a user up by username, ignoring upper/lower case. */
    Optional<User> findByUsername(String username);

    /** Replaces a person's stored password hash. */
    void updatePasswordHash(long userId, String newHash);
}
