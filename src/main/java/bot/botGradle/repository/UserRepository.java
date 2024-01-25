package bot.botGradle.repository;

import bot.botGradle.model.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends CrudRepository<User, Long> {

   // @Query("UPDATE u SET u.1 = u.userChoose = :userChoose WHERE u.chatId = :chatId")
//    void updateUserChoose(String userChoose, Long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.userChoose = :userChoose WHERE u.chatId = :chatId")
    void updateUserChooseByChatId(@Param("userChoose") String userChoose, @Param("chatId") Long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.userLanguage = :userLanguage WHERE u.chatId = :chatId")
    void updateUserLanguageByChatId(@Param("userLanguage") String userLanguage, @Param("chatId") Long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.userLastWord = :userLastWord WHERE u.chatId = :chatId")
    void updateUserLastWordByChatId(@Param("userLastWord") String userLastWord, @Param("chatId") Long chatId);

    @Query("SELECT u FROM User u WHERE u.chatId = :chatId")
    User findUserDataByChatId(long chatId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.userLastWordId = :userLastWordId WHERE u.chatId = :chatId")
    void updateUserLastWordIdByChatId(@Param("userLastWordId") int userLastWordId, @Param("chatId") Long chatId);


}
