package bot.botGradle.repository;

import bot.botGradle.model.Words;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WordsRepository extends JpaRepository<Words, Integer> {

  @Query("SELECT w FROM Words w ORDER BY RANDOM() LIMIT 1")
  Words findRandomWord();

  List<Words> findByWordRu(String word);

  List<Words> findByWordEn(String word);
}
