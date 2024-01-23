package bot.botGradle.repository;

import bot.botGradle.model.User;
import bot.botGradle.model.Words;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WordsRepository extends JpaRepository<Words, Integer> {

  @Query("SELECT w FROM Words w WHERE countWordEn < 5 ORDER BY RANDOM() LIMIT 1")
  Words findRandomWordEn();

  @Query("SELECT w FROM Words w WHERE countWordRu < 5 ORDER BY RANDOM() LIMIT 1")
  Words findRandomWordRu();

  List<Words> findByWordRu(String word);

  List<Words> findByWordEn(String word);

  @Modifying
  @Transactional
  @Query("UPDATE Words w SET w.countWordEn = :countWordEn WHERE w.id = :id")
  void updateCountWordEnByChatId(@Param("countWordEn") int countWordEn, @Param("id") int id);

  @Modifying
  @Transactional
  @Query("UPDATE Words w SET w.countWordRu = :countWordRu WHERE w.id = :id")
  void updateCountWordRuByChatId(@Param("countWordRu") int countWordRu, @Param("id") int id);

  @Query("SELECT w.countWordRu FROM Words w WHERE w.id = :id")
  int findCountWordRuByChatId(@Param("id") int id);

  @Query("SELECT w.countWordEn FROM Words w WHERE w.id = :id")
  int findCountWordEnByChatId(@Param("id") int id);

  @Modifying
  @Transactional
  @Query("UPDATE Words w SET w.countWordRu = 0")
  void updateCountWordRuByChatId();

  @Modifying
  @Transactional
  @Query("UPDATE Words w SET w.countWordEn = 0")
  void updateCountWordEnByChatId();

}
